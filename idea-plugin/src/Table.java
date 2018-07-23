import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.lang.ObjectUtils;

import java.io.IOException;

public class Table extends AnAction {

    public static void main(String[] args) {
        App.launch(App.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        App.launch(App.class);
    }

}
