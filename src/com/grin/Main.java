package com.grin;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.StreamGobbler;
import com.jcraft.jsch.*;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws Exception {
        executeLinux();
//        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
//
//        int returnValue = jfc.showOpenDialog(null);
//        // int returnValue = jfc.showSaveDialog(null);
//
//        if (returnValue == JFileChooser.APPROVE_OPTION) {
//            File selectedFile = jfc.getSelectedFile();
//            System.out.println(selectedFile.getAbsolutePath());
//        }
    }

    

    private static int pID;
    private static final String commandSearchPIDJar = "ps aux | grep demo-0.0.1-SNAPSHOT.jar";
    private static final String commandKillPId = "kill " + pID;
    private static final String commandNohup = "nohup java -jar demo-0.0.1-SNAPSHOT.jar $";

    private static final String localFile = "D:\\projects\\java\\javaEE\\netBeans\\univerServer\\target\\demo-0.0.1-SNAPSHOT.jar";
    private static final String destinationFile = "/root/demo-0.0.1-SNAPSHOT.jar";

    private static void executeLinux() throws JSchException {
        try {
            Connection conn = new Connection(host);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(user, password);
            if (!isAuthenticated) {
                throw new IOException("Authentication failed.");
            }

            searchPId(commandSearchPIDJar, conn);
            executeCommand(commandKillPId, conn);
            Sftp.upload(host, port, user, password, localFile, destinationFile);
            executeCommand(commandNohup, conn);

            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static void searchPId(String command, Connection conn) throws IOException {
        ch.ethz.ssh2.Session sess = conn.openSession();
        sess.execCommand(command);
        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);

            if (line.contains("java -jar demo-0.0.1-SNAPSHOT.jar $")) {
                List<String> items = Arrays.asList(line.split(" "));
                try {
                    pID = Integer.parseInt(items.get(5));
                } catch (Exception e) {
                    pID = Integer.parseInt(items.get(6));
                }
                System.out.println("pID = " + pID);
            }
        }
        System.out.println("ExitCode: " + sess.getExitStatus());
    }

    private static void executeCommand(String command, Connection conn) throws IOException {
        ch.ethz.ssh2.Session sess = conn.openSession();
        sess.execCommand(command);
        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);

        }
        System.out.println("ExitCode: " + sess.getExitStatus());
    }



}

class Sftp {

    private static long time0;

    public static void upload(String host, int port, String user, String password, String localFile, String destinationFile) throws JSchException {
        JSch jSch = new JSch();

        Session session = jSch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        System.out.println("session connected");

        Channel channel = session.openChannel("sftp");
        System.out.println("channel connected");
        channel.connect();
        ChannelSftp channelSftp = (ChannelSftp) channel;

        System.out.println("start");

        try {
            time0 = System.currentTimeMillis();
            channelSftp.put(localFile, destinationFile, new MyProgressMonitor(destinationFile), ChannelSftp.OVERWRITE);
        } catch (SftpException ex) {
            Logger.getLogger(Sftp.class.getName()).log(Level.SEVERE, null, ex);
        }

        channelSftp.exit();
        channel.disconnect();
    }

    private static class MyProgressMonitor implements SftpProgressMonitor {

        private String sourceFile;

        private long count;
        private long max;
        private long percent;

        public MyProgressMonitor(String sourceFile) {
            this.sourceFile = sourceFile;
        }

        @Override
        public void init(int i, String string, String string1, long l) {
            this.max = l;
            this.count = 0;
            this.percent = -1;
        }

        @Override
        public boolean count(long l) {
            this.count += l;

            if (percent >= count * 100 / max) {
                return true;
            }

            percent = count * 100 / max;

            if (percent % 10 == 0) {
                System.out.println(percent + " %");
            }

            return true;
        }

        @Override
        public void end() {
            System.out.println("progress: finished in " + (System.currentTimeMillis() - time0) + " ms");
        }

    }
}
