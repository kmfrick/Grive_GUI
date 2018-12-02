package grivegui.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

import java.io.*;

public class Controller {
    private static final String DRIVE_FOLDER = "Select your Google Drive folder";
    private static final String GRIVE_CMD = "grive";
    private static final String DRY_RUN = "--dry-run";
    private static final String UPLOAD = "-u";
    private static final String DOWNLOAD = "-n";
    private static final String INIT = "-a";
    private static final String PATH = "-p";
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.grive-gui/grive-gui.conf";
    private static final String INVALID_PATH = " is not a valid filepath.";
    private static final String ERR_CONF_RD = "Error reading config file.";
    private static final String ERR_CONF_WR = "Error appending line to config file.";
    private static final String ERR_CONF_CREATE = "Error creating config file.";
    private static final String NO_GRIVE_PATH = "No Grive path set.";
    public static final String INVALID_TOKEN = " is not a valid authorization token.";

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

    private File grivePath;
    private OutputStream griveOutputStream;
    private InputStream griveInputStream;
    private Process grive;


    public Controller() {
    }

    private void readConf() {
        createConf();
        try (BufferedReader f = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = f.readLine()) != null) {
                if (!folderCB.getItems().contains(line)) {
                    folderCB.getItems().add(line);
                }
                folderCB.setValue(line);
                grivePathStr = folderCB.getValue();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_RD).show();
        }
    }

    private void createConf() {
        File configFile = new File(CONFIG_FILE);
        try {
            if (!configFile.isFile()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_CREATE);
        }
    }

    private void chooseDriveDir(DirectoryChooser directoryChooser) {
        try {
            grivePath = directoryChooser.showDialog(setUpBtn.getScene().getWindow());
            grivePathStr = grivePath.getAbsolutePath();
        } catch (NullPointerException e) {
            new Alert(Alert.AlertType.ERROR, NO_GRIVE_PATH);
        }
        if (!folderCB.getItems().contains(grivePathStr)) {
            folderCB.getItems().add(grivePathStr);
        }
        folderCB.setValue(grivePathStr);
        writeGrivePathToConf();
    }

    private void writeGrivePathToConf() {
        try (PrintWriter f = new PrintWriter(new FileWriter(CONFIG_FILE))) {
            f.println(grivePathStr);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_WR).show();
        } catch (NullPointerException e) {
            new Alert(Alert.AlertType.ERROR, NO_GRIVE_PATH);
        }
    }


    private void runProcessLoggingOnTermTA(ProcessBuilder griveBuilder) throws IOException {
        griveBuilder.redirectErrorStream(true);
        grive = griveBuilder.start();
        griveInputStream = grive.getInputStream();
        griveOutputStream = grive.getOutputStream();

        Thread outputToTA = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(new InputStreamReader(griveInputStream))) {
                String line;
                while ((line = out.readLine()) != null) {
                    termTA.appendText(line + "\n");
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Error reading process output").show();
            }

        });
        outputToTA.start();
    }

    private void runGriveOnPathWithArguments(String grivePath, String arg) {
        tokenTF.setVisible(false);
        initConfirmBtn.setVisible(false);
        try {
            if (grivePath.length() > 0) {
                termTA.clear();
                ProcessBuilder griveBuilder = new ProcessBuilder(GRIVE_CMD, PATH + grivePath, arg);
                try {
                    runProcessLoggingOnTermTA(griveBuilder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                termTA.setText(grivePath + INVALID_PATH);
            }
        } catch (NullPointerException e) {
            new Alert(Alert.AlertType.ERROR, grivePath + INVALID_PATH).show();
        }
    }


    public void initialize() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(DRIVE_FOLDER);

        readConf();

        folderCB.setOnAction(e -> grivePathStr = folderCB.getValue());
        setUpBtn.setOnAction(e -> chooseDriveDir(directoryChooser));
        diffBtn.setOnAction(event -> runGriveOnPathWithArguments(grivePathStr, DRY_RUN));
        syncBtn.setOnAction(event -> runGriveOnPathWithArguments(grivePathStr, ""));
        dwldBtn.setOnAction(event -> runGriveOnPathWithArguments(grivePathStr, DOWNLOAD));
        upldBtn.setOnAction(event -> runGriveOnPathWithArguments(grivePathStr, UPLOAD));
        initBtn.setOnAction(event -> {
            runGriveOnPathWithArguments(grivePathStr, INIT);
            initConfirmBtn.setVisible(true);
            tokenTF.setVisible(true);
        });
        initConfirmBtn.setOnAction(e -> {
            Thread inputFromToken = new Thread(() -> {
                tokenTF.setVisible(false);
                initConfirmBtn.setVisible(false);
                try (PrintWriter in = new PrintWriter(new OutputStreamWriter(griveOutputStream))) {
                    in.println(tokenTF.getText());
                }
            });
            inputFromToken.start();
        });

        termTA.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue,
                                Object newValue) {
                termTA.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
                //use Double.MIN_VALUE to scroll to the tops
            }
        });


    }


}
