package util;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    // === configure these ===
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    // the Gmail you created for the app
    private static final String FROM_EMAIL    = "minesweeperwolf@gmail.com";  

    private static final String FROM_PASSWORD = "mcgu spdw dzof yrsg";


    public static void sendOtpEmail(String toEmail, String otpCode) throws MessagingException {
        // SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

        // auth session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        // build the message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(toEmail));
        message.setSubject("Your Mine Sweeper one-time password");

        String body = """
                Hello,

                Your one-time password (OTP) is: %s

                It is valid for the next 10 minutes.
                If you did not request this code, you can ignore this email.

                Mine Sweeper Smart
                """.formatted(otpCode);

        message.setText(body);

        // send it
        Transport.send(message);
    }
}
