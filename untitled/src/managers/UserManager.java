package managers;

import models.User;
import filters.UserFilter;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> users = new HashMap<>();

    // Метод репозитория
    @Override
    public void add(User user) {
        if (user == null)
            throw new IllegalArgumentException("User не может быть пустой");

        if (users.containsKey(user.username()))
            throw new IllegalArgumentException("User уже есть: " + user.username());

        users.put(user.username(), user);
    }

    @Override
    public boolean remove(User user) {
        return users.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    // Доп методы
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        User existing = users.get(username);

        if (existing == null)
            throw new NoSuchElementException("User не найден: " + username);

        User updated = User.validate(username, newFullName, newEmail);

        users.put(username, updated);
    }
}