package io.github.bluelhf.relly.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FileMonitor {
    private final File directory;
    private final Duration updatePeriod;
    private Timer timer = new Timer();
    private final HashMap<File, Buffers<String>> hashMap = new HashMap<>();
    private Consumer<IOException> exceptionConsumer = Throwable::printStackTrace;
    private BiConsumer<File, Change> changeConsumer;
    private ArrayList<File> fileBuffer = null;

    public FileMonitor(File directory, Duration updatePeriod, BiConsumer<File, Change> changeConsumer) {
        this.directory = directory;
        this.updatePeriod = updatePeriod;
        this.changeConsumer = changeConsumer;
    }

    public void setExceptionConsumer(Consumer<IOException> exceptionConsumer) {
        this.exceptionConsumer = exceptionConsumer;
    }

    public Consumer<IOException> getExceptionConsumer() {
        return exceptionConsumer;
    }

    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashMap<File, Change> files = new HashMap<>();
                try {
                    files = check();
                } catch (IOException e) {
                    exceptionConsumer.accept(e);
                }
                for (Map.Entry<File, Change> entry : files.entrySet()) {
                    changeConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
        }, 0, updatePeriod.toMillis());
    }

    private HashMap<File, Change> check() throws IOException {
        HashMap<File, Change> changed = new HashMap<>();
        List<File> files = getFiles();
        List<File> processed = new ArrayList<>();
        for (File file : files) {
            processed.add(file);
            if (!fileBuffer.contains(file)) {
                changed.put(file, Change.ADD);
            }
            String observation = DigestUtils.md5Hex(Files.newInputStream(file.toPath()));
            Buffers<String> buffers = hashMap.getOrDefault(file, new Buffers<>(observation));
            if (!buffers.same() && observation.equals(buffers.get())) {
                changed.put(file, Change.MODIFY);
                buffers.swap();
                buffers.set(observation);
            }
            hashMap.put(file, buffers);
        }
        for (File file : fileBuffer) {
            if (!processed.contains(file)) changed.put(file, Change.REMOVE);
        }

        fileBuffer.clear();
        fileBuffer.addAll(files);
        return changed;
    }

    private List<File> getFiles() {
        List<File> files;
        if (directory.isDirectory()) {
            files = Arrays.asList(directory.listFiles(File::isFile));
        } else {
            files = Collections.singletonList(directory);
        }
        if (fileBuffer == null) fileBuffer = new ArrayList<>(files);
        return files;
    }

    public void stop() {
        timer.cancel();
        timer = new Timer();
    }

    public BiConsumer getChangeConsumer() {
        return changeConsumer;
    }

    public void setChangeConsumer(BiConsumer changeConsumer) {
        this.changeConsumer = changeConsumer;
    }

    public enum Change {
        ADD,
        MODIFY,
        REMOVE
    }
}
