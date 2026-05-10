public class Main {
    private static final int THREAD_COUNT = 5;
    private static final int CALCULATION_LENGTH = 40;

    public static void main(String[] args) {
         System.out.print("\033[H\033[2J");
         System.out.flush();

        Thread[] threads = new Thread[THREAD_COUNT];

        // Создаем и запускаем потоки
        for (int i = 0; i < THREAD_COUNT; i++) {
            MyTask task = new MyTask(i, i, CALCULATION_LENGTH);
            threads[i] = new Thread(task, "Worker-" + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
