package src.managers;
import models.User;
import src.filters.UserFilter;
import src.repositories.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {
    private final Map<String, User> usersByUsername;
    private final ReentrantReadWriteLock lock;

    public UserManager() {
        this.usersByUsername = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String username = user.username();

        lock.writeLock().lock();
        try {
            if (usersByUsername.containsKey(username)) {
                throw new IllegalArgumentException("User with username '" + username + "' already exists");
            }
            usersByUsername.put(username, user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            return usersByUsername.remove(user.username()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return findByUsername(id);
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(usersByUsername.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return usersByUsername.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            usersByUsername.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(usersByUsername.get(username));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return usersByUsername.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return usersByUsername.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String username, String newFullName, String newEmail) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        lock.writeLock().lock();
        try {
            User existingUser = usersByUsername.get(username);
            if (existingUser == null) {
                throw new IllegalArgumentException("User with username '" + username + "' not found");
            }

            String updatedFullName = newFullName != null ? newFullName : existingUser.fullName();
            String updatedEmail = newEmail != null ? newEmail : existingUser.email();

            User updatedUser = User.validate(username, updatedFullName, updatedEmail);
            usersByUsername.put(username, updatedUser);
        } finally {
            lock.writeLock().unlock();
        }
    }
}