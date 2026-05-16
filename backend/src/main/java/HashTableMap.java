import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class HashTableMap<KeyType, ValueType> implements MapADT<KeyType, ValueType> {

    protected class Pair {

        public KeyType key;
        public ValueType value;

        public Pair(KeyType key, ValueType value) {
            this.key = key;
            this.value = value;
        }

    }

    protected LinkedList<Pair>[] table = null;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public HashTableMap(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        table = (LinkedList<Pair>[]) new LinkedList[capacity];
    }

    public HashTableMap() {
        this(8);
    }

    @Override
    public void put(KeyType key, ValueType value) throws IllegalArgumentException {

        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        int index = Math.abs(key.hashCode()) % table.length;

        if (table[index] != null) {
            for (Pair pair : table[index]) {
                if (pair.key.equals(key)) {
                    throw new IllegalArgumentException("Key already exists in the table");
                }
            }
        }

        if ((size + 1.0) / table.length >= 0.75) {
            LinkedList<Pair>[] oldTable = table;

            @SuppressWarnings("unchecked")
            LinkedList<Pair>[] newTable = (LinkedList<Pair>[]) new LinkedList[oldTable.length * 2];
            table = newTable;

            for (LinkedList<Pair> bucket : oldTable) {
                if (bucket != null) {
                    for (Pair pair : bucket) {
                        int newIndex = Math.abs(pair.key.hashCode()) % table.length;

                        if (table[newIndex] == null) {
                            table[newIndex] = new LinkedList<>();
                        }
                        table[newIndex].add(pair);
                    }
                }
            }
            index = Math.abs(key.hashCode()) % table.length;
        }

        if (table[index] == null) {
            table[index] = new LinkedList<>();
        }
        table[index].add(new Pair(key, value));
        size++;
    }

    @Override
    public boolean containsKey(KeyType key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        int index = Math.abs(key.hashCode()) % table.length;

        boolean hasKey = false;

        if (table[index] != null) {
            for (Pair pair : table[index]) {
                if (pair.key.equals(key)) {
                    hasKey = true;
                    break;
                }
            }
        }
        return hasKey;
    }

    @Override
    public ValueType get(KeyType key) throws NoSuchElementException {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        int index = Math.abs(key.hashCode()) % table.length;

        if (table[index] != null) {
            for (Pair pair : table[index]) {
                if (pair.key.equals(key)) {
                    return pair.value;
                }
            }
        }
        throw new NoSuchElementException("Key does not exist in the table");
    }

    @Override
    public ValueType remove(KeyType key) throws NoSuchElementException {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        int index = Math.abs(key.hashCode()) % table.length;

        if (table[index] != null) {
            for (int i = 0; i < table[index].size(); i++) {
                if (table[index].get(i).key.equals(key)) {
                    ValueType value = table[index].get(i).value;
                    table[index].remove(i);
                    size--;
                    return value;
                }
            }
        }
        throw new NoSuchElementException("Key does not exist in the table");
    }

    @Override
    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getCapacity() {
        return table.length;
    }

    @Override
    public List<KeyType> getKeys() {
        List<KeyType> keys = new LinkedList<>();

        for (LinkedList<Pair> bucket : table) {
            if (bucket != null) {
                for (Pair pair : bucket) {
                    keys.add(pair.key);
                }
            }
        }

        return keys;
    }


}
