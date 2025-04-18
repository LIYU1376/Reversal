package cn.stars.reversal.util.misc;

import cn.stars.reversal.Reversal;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import tech.skidonion.obfuscator.annotations.NativeObfuscation;

import java.io.*;
import java.util.Objects;

/**
 * Utilities for accessing and using files in the Rise directory.
 *
 * @author Strikeless
 * @since 08/06/2021
 */
@NativeObfuscation
@UtilityClass
public class FileUtil {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final String SEPARATOR = File.separator;

    private final String REVERSAL_PATH = mc.mcDataDir.getAbsolutePath() + SEPARATOR + "Reversal" + SEPARATOR;

    public boolean exists(final String fileName) {
        return getFileOrPath(fileName).exists();
    }

    public boolean exists(final File file) {
        return file.exists();
    }

    /**
     * Checks if the Rise directory exists.
     *
     * @return whether or not the Rise directory exists.
     */
    public boolean coreDirectoryExists() {
        return new File(REVERSAL_PATH).exists();
    }

    public static void unpackFile(File file, String name) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copy(Objects.requireNonNull(Reversal.class.getClassLoader().getResourceAsStream(name)), fos);
        fos.close();
    }

    /**
     * Saves a string into the specified file.
     * If the file does not exist this will create it automatically.
     *
     * @param fileName the file name inside the Rise directory
     * @param override whether or not we should override the file if it exists already
     * @param content  the string to write into the file
     * @return whether or not the file was saved successfully, this will also return false if it wasn't overridden.
     */
    public boolean saveFile(final String fileName, final boolean override, final String content) {
        BufferedWriter writer = null;
        try {
            final File file = getFileOrPath(fileName);
            if (!exists(file)) {
                createCoreDirectory();
                createFile(file);
            } else if (!override) {
                return false;
            }

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
        } catch (final Throwable t) {
            t.printStackTrace();
            return false;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (final Throwable t) {
                t.printStackTrace();
                throw new IllegalStateException("Failed to close writer!");
            }
        }

        return true;
    }

    /**
     * Loads a string from the specified file.
     * If the file does not exist this will return null.
     *
     * @param fileName the file name inside the Rise directory
     * @return the string loaded from the file.
     */
    public String loadFile(final String fileName) {
        try {
            final File file = getFileOrPath(fileName);
            if (!exists(file)) return null;

            final BufferedReader reader = new BufferedReader(new FileReader(file));
            String content = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                content += "\r\n" + line;
            }

            reader.close();

            return content;
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException("Failed to read file!");
        }
    }

    /**
     * Creates the Rise directory if absent.
     */
    public void createCoreDirectory() {
        new File(REVERSAL_PATH).mkdirs();
    }

    public void createDirectory(final String directoryName) {
        getFileOrPath(directoryName.replace("\\", SEPARATOR)).mkdirs();
    }

    public void createFile(final String fileName) {
        try {
            getFileOrPath(fileName).mkdirs();
            getFileOrPath(fileName).createNewFile();
        } catch (final Throwable t) {
            throw new IllegalStateException("Unable to create Core directory!", t);
        }
    }

    public File[] listFiles(final String path) {
        return getFileOrPath(path).listFiles();
    }

    public File[] listFiles(final File file) {
        return file.listFiles();
    }

    /**
     * Get a file object from the file name.
     *
     * @param fileName the file name inside the Rise directory
     * @return the file. duh.
     */
    public File getFileOrPath(final String fileName) {
        return new File(REVERSAL_PATH + fileName.replace("\\", SEPARATOR));
    }

    /**
     * Deletes the specified file if absent.
     *
     * @param fileName the file name inside the Rise directory
     */
    public void delete(final String fileName) {
        if (exists(fileName)) {
            if (!getFileOrPath(fileName).delete()) throw new IllegalStateException("Unable to delete file!");
        }
    }

    /**
     * Deletes the specified file if absent.
     *
     * @param file the file to delete
     */
    public void delete(final File file) {
        if (exists(file)) {
            if (!file.delete()) throw new IllegalStateException("Unable to delete file!");
        }
    }

    /**
     * Creates the specified file if absent.
     *
     * @param file the file to create
     */
    private void createFile(final File file) {
        try {
            file.createNewFile();
        } catch (final Throwable t) {
            throw new IllegalStateException("Unable to create file!", t);
        }
    }
}
