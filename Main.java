import java.util.*;
import java.util.concurrent.*;

public class Main {

    // Генерація масиву із заданими параметрами
    public int[][] generateArray(int rows, int cols, int min, int max) {
        Random random = new Random();
        int[][] array = new int[rows][cols];
        // Заповнення масиву випадковими значеннями
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                array[i][j] = random.nextInt(max - min + 1) + min; // Генерація числа в діапазоні [min, max]
            }
        }
        return array;
    }

    // Клас для пошуку через ForkJoinPool (Work stealing)
    static class ArraySearchTask extends RecursiveTask<String> {
        private final int[][] array; // Масив для обробки
        private final int startRow, endRow; // Межі рядків, які обробляє ця задача

        // Конструктор
        public ArraySearchTask(int[][] array, int startRow, int endRow) {
            this.array = array;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        // Основна логіка виконання задачі
        @Override
        protected String compute() {
            // Якщо завдання для обробки мале, обробляємо його напряму
            if (endRow - startRow <= 1) {
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < array[i].length; j++) {
                        // Перевірка умови: елемент == сумі його індексів
                        if (array[i][j] == i + j) {
                            return "Value: " + array[i][j] + " at index [" + i + "][" + j + "]";
                        }
                    }
                }
                return null; // Нічого не знайдено
            }

            // Розбиваємо завдання на підзадачі
            int mid = (startRow + endRow) / 2;
            ArraySearchTask leftTask = new ArraySearchTask(array, startRow, mid); // Ліва половина
            ArraySearchTask rightTask = new ArraySearchTask(array, mid, endRow); // Права половина

            // Запускаємо ліву задачу в окремому потоці
            leftTask.fork();

            // Виконуємо праву задачу і чекаємо на результат лівої
            String rightResult = rightTask.compute();
            String leftResult = leftTask.join();

            // Повертаємо перший знайдений результат
            return rightResult != null ? rightResult : leftResult;
        }
    }

    // Метод пошуку через ThreadPool (Work dealing)
    public String searchWithThreadPool(int[][] array, int threads) {
        // Створюємо пул потоків
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<String>> results = new ArrayList<>();

        // Розподіляємо завдання між потоками (один потік обробляє один рядок)
        for (int i = 0; i < array.length; i++) {
            final int row = i; // Закріплюємо поточний рядок
            results.add(executor.submit(() -> {
                // Перевіряємо всі елементи у рядку
                for (int j = 0; j < array[row].length; j++) {
                    if (array[row][j] == row + j) {
                        return "Value: " + array[row][j] + " at index [" + row + "][" + j + "]";
                    }
                }
                return null; // Нічого не знайдено
            }));
        }

        // Завершуємо роботу пулу потоків
        executor.shutdown();

        // Обробка результатів
        for (Future<String> result : results) {
            try {
                String value = result.get(); // Отримуємо результат
                if (value != null) return value; // Повертаємо перший знайдений результат
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace(); // Виводимо помилку у випадку виключення
            }
        }
        return null; // Нічого не знайдено
    }

    // Метод для виведення двовимірного масиву
    public void printArray(int[][] array) {
        for (int[] row : array) {
            System.out.println(Arrays.toString(row)); // Виводимо кожний рядок масиву
        }
    }

    // Головний метод програми
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Введення параметрів масиву
        System.out.println("Enter number of rows, columns, min value, and max value:");
        int rows = scanner.nextInt(); // Кількість рядків
        int cols = scanner.nextInt(); // Кількість стовпців
        int min = scanner.nextInt(); // Мінімальне значення
        int max = scanner.nextInt(); // Максимальне значення

        Main program = new Main();
        // Генерація масиву
        int[][] array = program.generateArray(rows, cols, min, max);
        System.out.println("Generated array:");
        program.printArray(array); // Виведення масиву

        // Вибір методу пошуку
        System.out.println("Choose method: 1 - ForkJoin (Work stealing), 2 - ThreadPool (Work dealing):");
        int choice = scanner.nextInt();

        // Замір часу виконання
        long startTime = System.nanoTime();
        String result = null;

        if (choice == 1) {
            // Пошук через ForkJoinPool
            ForkJoinPool pool = new ForkJoinPool();
            result = pool.invoke(new ArraySearchTask(array, 0, array.length));
        } else if (choice == 2) {
            // Пошук через ThreadPool
            result = program.searchWithThreadPool(array, 4); // 4 потоки
        }

        long endTime = System.nanoTime();

        // Виведення результату
        System.out.println("Result: " + (result != null ? result : "Not found"));
        System.out.println("Execution time: " + (endTime - startTime) / 1e6 + " ms");
    }
}
