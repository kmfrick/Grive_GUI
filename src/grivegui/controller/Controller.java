package grivegui.controller;

import grivegui.model.Grive;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.*;

public class Controller {
    public static final String CONFIG_FILE = System.getProperty("user.home") + "/.grive-gui/grive-gui.conf";
    private static final String DRIVE_FOLDER = "Select your Google Drive folder";
    private static final String DRY_RUN = "--dry-run";
    private static final String UPLOAD = "-u";
    private static final String DOWNLOAD = "-n";
    private static final String INIT = "-a";
    private static final String ERR_PROC_OUT = "Error reading process output";
    private static final String ERR_CONF_RD = "Error reading config file.";
    @FXML
    private Button setUpBtn;
    @FXML
    private ComboBox<String> folderCB;
    @FXML
    private Button upldBtn;
    @FXML
    private Button dwldBtn;
    @FXML
    private Button syncBtn;
    @FXML
    private Button diffBtn;
    @FXML
    private TextArea termTA;
    @FXML
    private TextField tokenTF;
    @FXML
    private Button initBtn;
    @FXML
    private Button initConfirmBtn;
    @FXML
    private BorderPane mainPane;

    private String grivePathStr;
    private Process grive;

    public Controller() {
    }

    private static String readConf(ComboBox<String> folderCB) {
        Grive.createConf();   // Creates config files in user home if they do not exist
        try (BufferedReader f = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = f.readLine()) != null) {
                if (!folderCB.getItems().contains(line)) {
                    folderCB.getItems().add(line);  // Adds every previous Grive path to the relevant combo box
                }
                folderCB.setValue(line);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_RD).show();
        }
        return folderCB.getValue();
    }

    private static String addNewDir(ComboBox<String> folderCB, DirectoryChooser directoryChooser, Window window) {
        File path = directoryChooser.showDialog(window);
        String pathStr = path.getAbsolutePath();
        if (!folderCB.getItems().contains(pathStr)) {
            folderCB.getItems().add(pathStr);
        }
        folderCB.setValue(pathStr);
        Grive.appendToConf(CONFIG_FILE, pathStr);
        return pathStr;
    }

    private static void bindInputToTextField(TextField textField, Process process) {
        Thread inputFromToken = new Thread(() -> {

            try (PrintWriter in = new PrintWriter(new OutputStreamWriter(process.getOutputStream()))) {
                in.println(textField.getText());
            }
        });
        inputFromToken.start();
    }

    private static Process redirectOutputToTextArea(Process process, TextArea outputTA) {
        // Input and output are run on separate threads
        // Output to TextArea is implemented here
        // Input from TextField is implemented in bindInputToTextField()
        Thread outputToTA = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = out.readLine()) != null) {
                    outputTA.appendText(line + "\n");
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, ERR_PROC_OUT).show();
            }

        });
        outputToTA.start();
        return process;
    }

    private static Process runClearingUI(Controller controller, String arg) {
        controller.clearUI();
        Process proc = null;
        try {
            proc = Grive.run(controller.grivePathStr, arg);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, controller.grivePathStr + Grive.INVALID_PATH).show();
        }
        return proc;
    }

    private void clearUI() {
        tokenTF.setVisible(false);
        initConfirmBtn.setVisible(false);
        termTA.clear();
    }

    private void startGrive(String arg) {
        Process process = runClearingUI(this, arg);
        grive = redirectOutputToTextArea(process, termTA);
    }

    public void initialize() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(DRIVE_FOLDER);

        grivePathStr = readConf(folderCB);
        folderCB.setOnAction(e -> {
            clearUI();
            grivePathStr = folderCB.getValue();
        });
        setUpBtn.setOnAction(e -> {
            clearUI();
            grivePathStr = addNewDir(folderCB, directoryChooser, setUpBtn.getScene().getWindow());
        });
        diffBtn.setOnAction(event -> startGrive(DRY_RUN));
        syncBtn.setOnAction(event -> startGrive(""));
        dwldBtn.setOnAction(event -> startGrive(DOWNLOAD));
        upldBtn.setOnAction(event -> startGrive(UPLOAD));
        initBtn.setOnAction(event -> {
            startGrive(INIT);
            initConfirmBtn.setVisible(true);
            tokenTF.setVisible(true);
        });
        initConfirmBtn.setOnAction(e -> {
            tokenTF.setVisible(false);
            initConfirmBtn.setVisible(false);
            bindInputToTextField(tokenTF, grive);
        });

        termTA.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) ->  // Autoscroll
                termTA.setScrollTop(Double.MAX_VALUE));
    }


}
