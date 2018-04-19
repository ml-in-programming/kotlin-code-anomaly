package io.github.ksmirenko.kotlin.visualizerGui.view

import io.github.ksmirenko.kotlin.visualizerGui.model.Model
import io.github.ksmirenko.kotlin.visualizerGui.model.UserResponse
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import tornadofx.*

class MainWindow : View("Code anomaly visualizer GUI") {
    override val root = VBox()

    private val selectDatasetButton: SelectDatasetButton by inject()
    private val anomalyCategoryLabel: AnomalyCategoryLabel by inject()
    private val anomalyIdLabel: AnomalyIdLabel by inject()
    private val anomalyContentView: AnomalySourceView by inject()
    private val buttonRow: LabelButtonRow by inject()

    init {
        root.prefWidth = 800.0

        root += selectDatasetButton.root
        root += anomalyCategoryLabel.root
        root += anomalyIdLabel.root
        root += anomalyContentView.root
        root += buttonRow.root
    }
}

class SelectDatasetButton : View() {
    override val root = button("Select dataset") {
        vboxConstraints {
            marginTopBottom(R.MARGIN_SMALL)
            marginLeftRight(R.MARGIN_SMALL)
        }
        action {
            val repoRootDirectory = chooseDirectory("Select root folder of the dataset")
            if (repoRootDirectory != null) {
                Model.openDataset(repoRootDirectory)
            }
        }
    }
}

class AnomalyCategoryLabel : View() {
    override val root = label {
        vboxConstraints {
            marginTopBottom(R.MARGIN_SMALL)
            marginLeftRight(R.MARGIN_SMALL)
        }
        bind(Model.categoryProperty)
    }
}

class AnomalyIdLabel : View() {
    override val root = label {
        vboxConstraints {
            marginLeftRight(R.MARGIN_SMALL)
        }
        bind(Model.idProperty)
    }
}

class AnomalySourceView : View() {
    override val root = scrollpane(fitToWidth = true, fitToHeight = true) {
        vboxConstraints {
            marginTopBottom(R.MARGIN_SMALL)
            marginLeftRight(R.MARGIN_SMALL)
        }
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        minHeight = 200.0
    }

    private val contentLabel = label("Anomaly source code will be here") {
        vboxConstraints {
            marginTopBottom(20.0)
            marginLeftRight(R.MARGIN_SMALL)
        }
        contentDisplay = ContentDisplay.TOP
        fitToParentHeight()
    }

    init {
        root += contentLabel
        contentLabel.bind(Model.sourceCodeProperty)
    }
}

class LabelButtonRow : View() {
    override val root = hbox {
        vboxConstraints { marginTopBottom(R.MARGIN_SMALL) }
        button("Want more") {
            hboxConstraints { marginLeftRight(R.MARGIN_SMALL) }
            action { Model.processUserResponse(UserResponse.WANT_MORE) }
        }
        button("Enough") {
            hboxConstraints { marginLeftRight(R.MARGIN_SMALL) }
            action { Model.processUserResponse(UserResponse.ENOUGH) }
        }
        button("Useless") {
            hboxConstraints { marginLeftRight(R.MARGIN_SMALL) }
            action { Model.processUserResponse(UserResponse.USELESS) }
        }
    }
}