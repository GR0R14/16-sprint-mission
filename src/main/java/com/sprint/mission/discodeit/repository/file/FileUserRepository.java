package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileUserRepository implements UserRepository {

    // 사용자 객체 파일들을 저장할 디렉터리입니다.
    private final Path directory;

    // 기본 경로를 사용합니다.
    public FileUserRepository() {
        this(Paths.get(System.getProperty("user.dir"), "data", "users"));
    }

    // 경로를 직접 전달할 수도 있습니다.
    public FileUserRepository(Path directory) {
        this.directory = directory;

        try {
            // 디렉터리가 없으면 생성합니다.
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "사용자 저장 디렉터리를 만들 수 없습니다: " + directory, e
            );
        }
    }

    @Override
    public void save(User user) {
        // UUID.ser 파일에 User 객체를 직렬화해 저장합니다.
        write(fileFor(user.getId()), user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        Path file = fileFor(id);

        // 파일이 없으면 사용자가 없는 것으로 Optional.empty()를 반환합니다.
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        // 파일이 있으면 역직렬화한 User를 Optional에 담습니다.
        return Optional.of(read(file));
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        // 디렉터리의 .ser 파일을 모두 읽어 User 목록을 만듭니다.
        try (var files = Files.list(directory)) {
            files.filter(path ->
                            path.getFileName().toString().endsWith(".ser"))
                    .forEach(path -> users.add(read(path)));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "사용자 파일 목록을 읽을 수 없습니다: " + directory, e
            );
        }

        return List.copyOf(users);
    }

    @Override
    public boolean existsById(UUID id) {
        // ID에 해당하는 파일이 있는지 확인합니다.
        return Files.exists(fileFor(id));
    }

    @Override
    public boolean deleteById(UUID id) {
        try {
            // 삭제하면 true, 해당 파일이 없으면 false를 반환합니다.
            return Files.deleteIfExists(fileFor(id));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "사용자 파일을 삭제할 수 없습니다: " + id, e
            );
        }
    }

    private Path fileFor(UUID id) {
        // UUID를 파일명으로 사용합니다.
        return directory.resolve(id + ".ser");
    }

    private void write(Path file, User user) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(user);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "사용자 파일을 저장할 수 없습니다: " + file, e
            );
        }
    }

    private User read(Path file) {
        try (ObjectInputStream in =
                     new ObjectInputStream(Files.newInputStream(file))) {
            return (User) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(
                    "사용자 파일을 읽을 수 없습니다: " + file, e
            );
        }
    }
}