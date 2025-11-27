package control;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

public class WinController {

	
	@FXML
	private GridPane root;

	@FXML
	private void initialize() {
	    util.UIAnimations.applyHoverZoomToAllButtons(root);
	    util.UIAnimations.applyFloatingToCards(root);
	}

}
