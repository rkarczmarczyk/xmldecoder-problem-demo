package com.example.xmldecoder;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class XmlDecoderApplication {

    private Path holderPath = Paths.get("opt", "files_to_load");

    public static void main(String[] args) {
        SpringApplication.run(XmlDecoderApplication.class, args);
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 1000)
    public void readFiles() {
        FileReader reader = new FileReader(ActorSystem.create(), new ReadJob(holderPath));
        reader.loadFiles();
    }
}

@Slf4j
@RequiredArgsConstructor
class FileReader {

    private final ActorSystem system;
    private final ReadJob readJob;

    public NotUsed loadFiles() {
        List<String> paths = listFiles(readJob);
        return Source.from(paths)
                .via(Flow.of(String.class).mapAsync(5, p -> loadFile(p)))
                .to(Sink.foreach(System.out::println)).run(ActorMaterializer.create(system));
    }

    private CompletionStage<String> loadFile(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileInputStream fis2 = new FileInputStream(filePath);
                BufferedInputStream bis2 = new BufferedInputStream(fis2);
                XMLDecoder xmlDecoder = new XMLDecoder(bis2);
                FileDto mb = (FileDto) xmlDecoder.readObject();
                log.info("Decoder: {}", mb);
                return mb.toString();
            } catch (Exception e) {
                log.error("Unexpected exception in load file, {}", e);
                throw new RuntimeException("Unexpected exception in load file", e);
            }
        });
    }

    private List<String> listFiles(ReadJob readJob) {
        File folder = new File(readJob.getHolderDirPath().toString());
        File[] listOfFiles = folder.listFiles();
        log.info(listOfFiles.toString());
        return Stream.of(listOfFiles).map(File::getAbsolutePath).collect(Collectors.toList());
    }
}

@Getter
@RequiredArgsConstructor
class ReadJob {
    private final Path holderDirPath;
}

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
class FileDto {
    String field1;
    String field2;
}
