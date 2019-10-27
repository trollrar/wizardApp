package si.fullin.wizardapp;

public abstract class ServerThread extends Thread {

    @Override
    public void run() {
        try {
            ApiService apiService = new ApiService();
            apiService.getStatus(this::playerAction);

            this.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    abstract void playerAction(String spell);
}
