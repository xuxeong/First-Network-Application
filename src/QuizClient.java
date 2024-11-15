/*
 * File name: QuizClient.java
 * Author: 최수정
 * Date: 2024.11.15
 * Summary: This file is Quiz Client program that connect to the server, receive questions, provide answers, display feedback and final score.
 */

// 필요한 패키지 불러오기
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class QuizClient extends JFrame {
    private JTextField questionField; 
    private JTextField answerField; 
    private JButton submitButton; 
    private JButton nextQuestionButton; 
    private JLabel feedbackLabel; 
    private BufferedReader in; 
    private PrintWriter out;

    public QuizClient() {
        // server_info.dat파일에 서버 정보가 없을 시 사용할 기본주소
        String serverAddress = "localhost"; // 기본값
        int port = 2024; // 기본 포트
        
        //server_info.dat파일의 서버정보로 Server 연결
        try (BufferedReader reader = new BufferedReader(new FileReader("src/server_info.dat"))) {
            serverAddress = reader.readLine();
            port = Integer.parseInt(reader.readLine()); //입력받은 문자열을 정수로 변형
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "server_info.dat 파일을 찾을 수 없어 기본 서버 설정을 사용합니다.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }

        // GUI 초기화
        setTitle("Quiz Game Client");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //창을 닫으면 프로그램 종료
        setLayout(new BorderLayout());
        
        // 질문 표시할 영역 생성 및 배치
        questionField = new JTextField();
        questionField.setEditable(false); //사용자 접근 방지
        add(new JScrollPane(questionField), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new GridLayout(4,1));
        JPanel answerPanel = new JPanel(new FlowLayout());

        // 답변 입력 필드 생성
        answerField = new JTextField(20);
        inputPanel.add(new JLabel("Your answer:"));
        inputPanel.add(answerField);

        // 제출 버튼 생성
        submitButton = new JButton("Submit Answer");
        inputPanel.add(submitButton);
        
        // 다음문제 버튼 생성
        nextQuestionButton = new JButton("Next");
        nextQuestionButton.setEnabled(false); //처음엔 비활성화

        // 피드백 표시할 영역 생성
        feedbackLabel = new JLabel(" ");
        
        inputPanel.add(answerPanel); // 답변 필드와 라벨을 첫번째줄에
        inputPanel.add(submitButton); // 제출 버튼을 두번째줄에
        inputPanel.add(nextQuestionButton); // 다음문제 버튼 세번째줄에
        inputPanel.add(feedbackLabel); // 피드백을 하단에
        

        add(inputPanel, BorderLayout.SOUTH);

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAnswer();
                nextQuestionButton.setEnabled(true); // 답변 제출 후 next버튼 활성화
                submitButton.setEnabled(false); // 답변 제출 후 제출버튼 비활성화
            }
        });
        nextQuestionButton.addActionListener(new ActionListener() {
        	@Override
            public void actionPerformed(ActionEvent e) {
                out.println("NEXT");  // 서버로 "NEXT" 메시지를 보냄
                nextQuestionButton.setEnabled(false);  // next 버튼 비활성화
                submitButton.setEnabled(true); // 제출버튼 활성화
            }
        });


        // 서버 연결 시도
        try {
            Socket socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new QuizListener()).start(); //QuizListener 스레드 생성
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 사용자가 입력한 답변을 서버로 전송
    private void sendAnswer() {
        String answer = answerField.getText();
        if (!answer.isEmpty()) {
            out.println("ANSWER:" + answer);
            answerField.setText(""); // 입력 필드를 비움
        }
    }

    // 서버로부터 받은 메세지 처리
    private class QuizListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                	System.out.println("Received from server: " + serverMessage);
                	//QUESTION 프로토콜
                    if (serverMessage.startsWith("QUESTION:")) {
                        String question = serverMessage.substring(9); //"QUESTION:"이후 실제 질문내용만 추출
                        questionField.setText(question);
                        feedbackLabel.setText(" ");
                    }
                    //FEEDBACK 프로토콜
                    else if (serverMessage.startsWith("FEEDBACK:")) {
                        String feedback = serverMessage.substring(9); //"FEEDBACK:"이후 실제 피드백내용만 추출
                        feedbackLabel.setText(feedback);
                    } 
                    //SCORE 프로토콜
                    else if (serverMessage.startsWith("SCORE:")) {
                        String score = serverMessage.substring(6); //"SCORE:"이후 실제 점수만 추출
                        JOptionPane.showMessageDialog(QuizClient.this, "Your final score is: " + score);
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
    	//GUI작업은 Event Dispatch Thread 에서 처리해야 안전.
    	SwingUtilities.invokeLater(new Runnable() {
    	    @Override
    	    public void run() {
    	        new QuizClient().setVisible(true); //GUI창을 화면에 표시
    	    }
    	});

    }
}
