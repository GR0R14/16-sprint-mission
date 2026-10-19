package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileUserService implements UserService {
    private final Path directory;

    public FileUserService() {
        this(Paths.get(System.getProperty("user.dir"), "data", "users"));
    }

    public FileUserService(Path directory) {
        this.directory = directory;

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create directory ", e);
        }
    }

    @Override
    public void create(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Path file = fileFor(user.getId());

        if (Files.exists(file)) {
            throw new IllegalArgumentException("User already exists" + user.getId());
        }

        validateEmail(user.getEmail());

        if (isEmailDuplicate(user.getEmail(), null)) {
            throw new IllegalArgumentException(
                    "Email already exists" + user.getEmail()
            );
        }

        write(file, user);
    }

    @Override
    public User findById(UUID id) {
        Path file = fileFor(id);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Not Found user id" + id);
        }

        return read(file);
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".ser"))
                    .forEach(path -> users.add(read(path)));
        } catch (Exception e) {
            throw new IllegalStateException("사용자 파일 목록을 읽을 수 없습니다.",e);
        }

        return List.copyOf(users);
    }

    @Override
    public void update(UUID id, User user) {
        User existingUser = findById(id);

        validateEmail(user.getEmail());

        if (isEmailDuplicate(user.getEmail(), id)) {
            throw new IllegalArgumentException(
                    "Email already exists" + user.getEmail()
            );
        }

        existingUser.update(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );

        write(fileFor(id), existingUser);
    }

    @Override
    public void delete(UUID id) {
        Path file = fileFor(id);

        try {
            if (!Files.deleteIfExists(file)) {
                throw new IllegalArgumentException("Not Found user id" + id);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete user id" + id, e);
        }
    }

    private Path fileFor(UUID id) {
        return directory.resolve(id + ".ser");
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 비어 있을 수 없습니다.");
        }

        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailPattern)) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + email);
        }
    }

    private boolean isEmailDuplicate(String email, UUID excludeUserId) {
        return findAll().stream()
                .anyMatch(user ->
                        user.getEmail().equalsIgnoreCase(email)
                                && !user.getId().equals(excludeUserId)
                );
    }

    private void write(Path file, User user) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(user);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write user " + file, e);
        }
    }

    private User read(Path file) {
        try (ObjectInputStream in =
                     new ObjectInputStream(Files.newInputStream(file))) {
            return (User) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to read user " + file, e);
        }
    }
}
