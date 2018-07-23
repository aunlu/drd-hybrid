import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public class Data {
        SimpleStringProperty name;
        SimpleIntegerProperty num;
        Data(String s, int n) {
            name = new SimpleStringProperty(s);
            num = new SimpleIntegerProperty(n);
        }
        public String getName() {
            return name.get();
        }
        public void setName(String name1) {
            name.set(name1);
        }
        public Integer getNum() {
            return num.get();
        }
        public void setNum(Integer num1) {
            num.set(num1);
        }
    }

    static Stage st;

    @Override
    public void start(Stage stage) {
        st = stage;
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setAlwaysOnTop(true);

        stage.setTitle("DRD Table");
        stage.setHeight(600);
        stage.setWidth(800);

        TableView<Data> table = new TableView<Data>();
        table.setEditable(true);

        TableColumn <Data, String> leftColumn = new TableColumn<Data, String>("Column #1");
        leftColumn.setPrefWidth(390);
        leftColumn.setCellValueFactory(
                new PropertyValueFactory<Data, String>("name"));

        TableColumn <Data, Integer> rightColumn = new TableColumn<Data, Integer>("Column #2");
        rightColumn.setPrefWidth(390);
        rightColumn.setCellValueFactory(
                new PropertyValueFactory<Data, Integer>("num"));


        ObservableList<Data> dataMembers = FXCollections.observableArrayList(new Data("Happy New Year ", 2016),
                new Data("Happy New Year ", 2016),
                new Data("Happy New Year ", 2016));

        table.setItems(dataMembers);

        table.getColumns().addAll(leftColumn, rightColumn);

        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        vbox.getChildren().add(table);

        ((Group) scene.getRoot()).getChildren().add(vbox);

        stage.setScene(scene);
        stage.show();
    }
}
