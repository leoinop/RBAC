public class MyTask implements Runnable {
    private static final int STEP_DELAY_MS = 100;
    private final int threadNumber;
    private final int countCalc;
    private final int rowIndex; // Номер строки в консоли (для позиционирования курсора)

    public MyTask(int threadNumber, int rowIndex, int countCalc) {
        this.threadNumber = threadNumber;
        this.rowIndex = rowIndex;
        this.countCalc = countCalc;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long threadId = Thread.currentThread().threadId();
        try {
            for (int i = 0; i < countCalc; i++) {

                Thread.sleep(STEP_DELAY_MS);
                String progressBar = buildProgressBar(i, countCalc);
                String timeStatus = " " + (System.currentTimeMillis() - startTime);

                String outputLine = "Поток" + (threadNumber + 1) + " ID:" + threadId + " " + progressBar + " Time:" + timeStatus + "ms\n";
                // ВЫВОД В КОНСОЛЬ
                synchronized (System.out) {
                    System.out.print("\033[" + (rowIndex + 1) + ";1H");
                    System.out.print("\033[2K");
                    System.out.print(outputLine);
                    System.out.flush();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildProgressBar(int current, int total) {
        int barLength = 15;
        int filledLength = (int) (barLength * ((double) current / total));

        StringBuilder sb = new StringBuilder("[");
        for (int j = 0; j < barLength; j++) {
            if (j < filledLength) {
                sb.append("=");
            } else {
                sb.append(".");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
