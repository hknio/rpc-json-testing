package test_drivers;

import connection.Client;
import connection.ResponseObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GanacheTestDriver extends TestDriver {

    private List<String> accounts;
    private List<String> keys;

    public GanacheTestDriver(Client client) {
        super(client);
    }

    public void startServer() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();

        pb.command("/blockchain-testing/scripts/startGanache.sh");

        pb.redirectErrorStream(true);

        Process p = pb.start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            p.destroy();
        }
    }

    private void retrieveAccounts() throws IOException {
        accounts = new ArrayList<>();
        keys = new ArrayList<>();
        try {
            File myObj = new File("/ganache-cli/output.txt");
            Scanner myReader = new Scanner(myObj);

            boolean scanningAccounts = false;
            boolean scanningKeys = false;
            System.out.println("File exists! Start reading");
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println("--------- Start of the streamdata:");
                System.out.println(data);
                System.out.println("--------- END of the streamdata:");

                if (data.length() == 0) {
                    System.out.println("The length of the line = " + data.length());
                    scanningAccounts = false;
                    scanningKeys = false;
                    continue;
                }

                if (data.contains("Available Accounts")) {
                    System.out.println("Data actually contains the accounts!");
                    scanningAccounts = true;
                    myReader.nextLine();
                    continue;
                }

                if (scanningAccounts) {
                    System.out.println("Account that is added: " + data.split(" ")[1]);
                    accounts.add(data.split(" ")[1]);
                }

                if (data.contains("Private Keys")) {
                    scanningKeys = true;
                    myReader.nextLine();
                    continue;
                }

                if (scanningKeys) {
                    System.out.println("Private key that is added: " + data.split(" ")[1]);
                    keys.add(data.split(" ")[1]);
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            e.printStackTrace();
        }
    }

    public void prepTest() throws Exception {
        startServer();
        retrieveAccounts();
    }

    protected JSONObject replaceAccountStrings(JSONObject request, String account) {
        return new JSONObject(request.toString().replace("__ACCOUNT__", account));
    }

    public ResponseObject runTest(String method, JSONObject request) throws Exception {
        if (accounts == null) {
            throw new Exception("No accounts found! Please call prepTest before runTest!!");
        }

        System.out.println("The list: " + accounts.toString());
        request = replaceAccountStrings(request,  accounts.get(0));

        // TODO something with private keys as well
        return getClient().createRequest(method, request);
    }

}
