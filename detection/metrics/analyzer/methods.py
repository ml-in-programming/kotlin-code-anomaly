import csv
import matplotlib.pyplot as plt
import numpy as np
import os
import pandas
import re
import sys
import time
from functools import reduce

# noinspection PyUnresolvedReferences
from mpl_toolkits.mplot3d import Axes3D
from sklearn.decomposition import PCA
from sklearn.model_selection import ParameterGrid
from sklearn.neighbors import LocalOutlierFactor
from sklearn.preprocessing import scale
from sklearn.svm import OneClassSVM

dataset_name = sys.argv[1]
is_drawing = False

csv_in_path = f"data/{dataset_name}.csv"
out_path = f"out-data/{dataset_name}/"
log_path = f"{out_path}methods.log"
# paths updated for new project structure
if not os.path.exists(out_path):
    os.makedirs(out_path)
log_file = open(log_path, mode='w+')


def log(s):
    print(s)
    log_file.write(s)
    log_file.write('\n')


def parse_timediff(timediff):
    h = timediff // 3600
    m = timediff % 3600 // 60
    s = timediff % 60
    return h, m, s


# Load input
methods = pandas.read_csv(csv_in_path, header=0, delimiter='\t', quoting=csv.QUOTE_NONE, error_bad_lines=True,
                          engine='python')
print("Done reading CSV.")

# Ensure validity of the input
X = np.array(methods.values[:, 2:], dtype="float64")
n_methods = methods.shape[0]
has_bad_lines = reduce(lambda a, x: a | x, [np.isnan(row).any() for row in X])
assert not has_bad_lines

# Preprocessing
X = scale(X)
X = PCA(n_components=5).fit_transform(X)
print("Done transforming data.")

# All configs
all_clf_configs = [
    {
        'clf_name': 'lof',
        'clf': LocalOutlierFactor(n_jobs=-1),
        'param_grid': {
            'n_neighbors': [10, 20],
            'contamination': [0.001, 0.0001, 0.00001]
        }
    },
    {
        'clf_name': 'svm',
        'clf': OneClassSVM(shrinking=True),
        'param_grid': [
            # {
            #     'kernel': ['linear'],
            #     'nu': [0.005]
            # },
            {
                'kernel': ['rbf'],
                'nu': [0.001, 0.0001],
                'gamma': ['auto']
            }
        ]
    }
]
# Configs for the current run
clf_configs = [clf_config for clf_config in all_clf_configs if clf_config['clf_name'] in ('lof', 'svm')]

for clf_config in clf_configs:
    clf_name = clf_config['clf_name']
    clf = clf_config['clf']
    param_sets = list(ParameterGrid(clf_config['param_grid']))

    # For calculating 'intersection', i.e. methods marked as anomalous
    # by the current classifier with all param sets
    all_indices = np.arange(0, n_methods)
    intersect_outlier_indices = all_indices
    union_outlier_indices = set()

    for params in param_sets:
        local_start = time.time()

        params_desc = re.sub("[{'}:]", "", str(params).replace(' ', '_'))
        clf_desc = f"{clf_name}_{params_desc}"
        log(f"{clf_desc}")

        # Fit the model and mark data
        clf.set_params(**params)
        if clf_name == 'lof':
            marks = clf.fit_predict(X)
        elif clf_name == 'svm':
            clf.fit(X)
            # Suppressed warning below: clf is in dictionary
            # noinspection PyUnresolvedReferences
            marks = clf.predict(X)
        else:
            log(f"Error: unknown classifier name {clf_name}!")
            exit(1)

        # Suppressed warning below: either `marks` is assigned, or the whole program exits with an error
        # noinspection PyUnboundLocalVariable
        inlier_indices = np.asarray([mark > 0 for mark in marks])
        outlier_indices = np.asarray([mark < 0 for mark in marks])
        intersect_outlier_indices = np.intersect1d(intersect_outlier_indices, all_indices[outlier_indices])
        union_outlier_indices = union_outlier_indices.union(all_indices[outlier_indices])

        X_inliers = X[inlier_indices]
        X_outliers = X[outlier_indices]
        n_inliers = X_inliers.shape[0]
        n_outliers = X_outliers.shape[0]
        log(f"\tInliers:\t{n_inliers:10}/{n_methods:10}\t{(n_inliers * 100 / n_methods):11.7}%")
        log(f"\tOutliers:\t{n_outliers:10}/{n_methods:10}\t{(n_outliers * 100 / n_methods):11.7}%")

        if n_outliers > n_inliers:
            X_temp = X_inliers
            X_inliers = X_outliers
            X_outliers = X_temp
            log("\tSwapped 'inliers' and 'outliers', because there were more outliers than inliers!")

        hours, minutes, seconds = parse_timediff(time.time() - local_start)
        log(f"Elapsed time: {hours} h. {minutes} min. {seconds} sec.\n")

        if is_drawing:
            # Show the principal components on 3D plot
            fig = plt.figure()
            ax = fig.add_subplot(111, projection='3d')
            ax.scatter(X_inliers[:, 1], X_inliers[:, 0], X_inliers[:, 2], c='None', edgecolor='blue', marker='o')
            ax.scatter(X_outliers[:, 1], X_outliers[:, 0], X_outliers[:, 2], c='red', marker='^')
            plt.savefig(f"{out_path}{clf_desc}.png")

        # Save output of this configuration to file
        outlier_names = methods.values[outlier_indices]
        dataframe = pandas.DataFrame(outlier_names)
        dataframe.to_csv(f"{out_path}{clf_desc}.csv", header=False, index=False)

    # Save the 'intersection' to file
    intersect_outlier_names = methods.values[intersect_outlier_indices]
    dataframe = pandas.DataFrame(intersect_outlier_names)
    dataframe.to_csv(f"{out_path}{clf_name}_intersection.csv", header=False, index=False)
    # Save the 'union' to file
    union_outlier_indices = list(union_outlier_indices)
    union_outlier_names = methods.values[union_outlier_indices]
    dataframe = pandas.DataFrame(union_outlier_names)
    dataframe.to_csv(f"{out_path}{clf_name}_union.csv", header=False, index=False)

log_file.close()
