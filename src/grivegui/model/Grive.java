package grivegui.model;

import grivegui.controller.Controller;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Grive {
    public static final String INVALID_PATH = " is not a valid filepath.";
    private static final String ERR_CONF_WR = "Error appending line to config file.";
    private static final String ERR_CONF_CREATE = "Error creating config file.";
    private static final String NO_GRIVE_PATH = "No path set.";
    private static final String GRIVE_CMD = "grive";
    private static final String PATH = "-p";

    public static boolean createConf() {
        File configFile = new File(Controller.CONFIG_FILE);
        boolean success = false;
        try {
            if (!configFile.isFile()) {
                success = configFile.getParentFile().mkdirs();
                success = success && configFile.createNewFile();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_CREATE);
            success = false;
        }
        return success;
    }

    public static void appendToConf(String configFile, String stringToAppend) {
        try (PrintWriter f = new PrintWriter(new FileWriter(configFile, true))) {
            f.println(stringToAppend);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, ERR_CONF_WR).show();
        } catch (NullPointerException e) {
            new Alert(Alert.AlertType.ERROR, NO_GRIVE_PATH);
        }
    }

    public static Process run(String grivePath, String arg) throws IOException {
        ProcessBuilder griveBuilder = new ProcessBuilder(GRIVE_CMD, PATH + grivePath, arg);
        griveBuilder.redirectErrorStream(true);
        return griveBuilder.start();
    }
}
