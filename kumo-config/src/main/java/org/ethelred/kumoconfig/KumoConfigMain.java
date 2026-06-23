// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import java.io.IOException;
import java.nio.file.Path;

public class KumoConfigMain {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("No console available - run this from an interactive terminal "
                    + "(see the Verification section in the implementation plan for how to invoke it).");
            System.exit(1);
            return;
        }

        var username = console.readLine("Enter username: ");
        var passwordChars = console.readPassword("Enter password: ");
        if (username == null || passwordChars == null) {
            System.err.println("No input received.");
            System.exit(1);
            return;
        }
        var password = new String(passwordChars);

        var outputPath = Path.of("kumo.cfg");
        try {
            var responseBody = new KumoCloudClient().login(username, password);
            var result = new KumoCloudConfigParser().parse(responseBody);
            new KumoConfigWriter().write(outputPath, result.accountName(), result.devices());
            System.out.printf(
                    "Downloaded config for %d devices. Written to ./kumo.cfg%n",
                    result.devices().size());
        } catch (KumoCloudException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Failed to write " + outputPath + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
