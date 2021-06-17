import Model.PersonModel;
import com.google.api.services.people.v1.model.Person;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mortbay.util.IO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {
    private Main main;
    private ObservableList<PersonModel> observableListToView = FXCollections.observableArrayList();
    private List<PersonModel> personModelListFromGoogle;
    private List<PersonModel> personModelListFromTxtFile = new ArrayList<>();

    @FXML
    private Button buttonLoad;
    @FXML
    private Button buttonSearch;
    @FXML
    private Label labelAllItem;
    @FXML
    private Label labelTable;
    @FXML
    private Label labelFileName;
    @FXML
    private TextField textFieldSearch;
    @FXML
    private TableView<PersonModel> tableView;
    @FXML
    private TableColumn<Person, String> nameColumn;
    @FXML
    private TableColumn<Person, String> phoneNumberColumn;

    @FXML
    private void buttonLoad() {
        final FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(main.getPrimaryStage());
        if (file != null) {
            try {
                labelFileName.setText(file.toString());
                loadPersonsFromTxtFile(file);
            } catch (Exception e) {

            }
        }
    }

    @FXML
    private void buttonSearch() {
        ObservableList<PersonModel> result = FXCollections.observableArrayList();
        String textFromField = textFieldSearch.getText();

        for (PersonModel personModel : observableListToView) {
            if (personModel.getName().contains(textFromField) ||
                    personModel.getPhoneNumber().contains(textFromField)) {
                result.add(personModel);
            }
        }

        tableView.setItems(result);
    }

    public void setTextLabelAllItem(String text) {
        labelAllItem.setText(text);
    }


    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneNumberColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        loadPersonsFromGoogle();
    }

    private void loadPersonsFromTxtFile(File file) {
        personModelListFromTxtFile.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.ready()) {
                String line = reader.readLine();
                Person person = PeopleContact.getPersonFromString(line);
                if (person != null) {
                    PersonModel personModel = new PersonModel(person);
                    personModelListFromTxtFile.add(personModel);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        main.getPrimaryStage().setTitle(main.getPrimaryStage().getTitle() + " : " + file.toString());
        refreshTable("Contact from txt file:", personModelListFromTxtFile);

    }

    private void loadPersonsFromGoogle() {
        try {
            setTextLabelAllItem("loading");
            personModelListFromGoogle = PersonModel.getPersonModelFromPerson(PeopleContact.getPersons());
            refreshTable("Contacts from Google Account:", personModelListFromGoogle);
            labelAllItem.setTextFill(Color.BLACK);
            setTextLabelAllItem(personModelListFromGoogle.size() + "");
        } catch (IOException e) {
            e.printStackTrace();
            sendMessage("Неудалось загрузить контакты");
            labelAllItem.setTextFill(Color.valueOf("#00000080"));
            setTextLabelAllItem("---");
        }
    }

    private void refreshTable(String title, List<PersonModel> list) {
        observableListToView.clear();

        observableListToView.addAll(list);
        labelTable.setText(title);
        tableView.setItems(observableListToView);

    }

    public void setMain(Main main) {
        this.main = main;
    }

    public void sendMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Exception");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
