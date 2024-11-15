/*
 * File name: QuizServer.java
 * Author: 최수정
 * Date: 2024.11.15
 * Summary: This file is Quiz Server program that store a set of questions and answers, send questions to the client, feedback and the final score, evaluate each response, and update the client's score.
 */

// 필요한 패키지 불러오기
import java.io.*;
import java.net.*;
import java.util.concurrent.*; //ExecutorService, Executors

public class QuizServer {
	public static void main(String[] args) throws Exception{

		ServerSocket listener = null;
		Socket socket = null;
		
		listener = new ServerSocket(2024); //서버소켓 생성
		System.out.println("Start QuizServer...");
		System.out.println("Waiting for clients");
		
		ExecutorService pool = Executors.newFixedThreadPool(5); //클라이언트 요청을 도시에 처리하기 위함(최대 5명까지 가능)
		while(true) {
			socket = listener.accept(); //클라이언트 연결 대기
			pool.execute(new Quiz(socket)); //새 클라이언트 연결에 대해 Quiz 실행
		}
	}
	
	
	private static class Quiz implements Runnable{
		private Socket socket;
		int score = 0;
		int currentQuestionIndex = 0;
		
		//생성자.
		Quiz(Socket socket){
			this.socket = socket;
		}
		
		//문제 세트
		String[] question = {
			"What is the Capital of the South Korea?",
			"초등학생이 가장 좋아하는 동네는?(띄어쓰기X)",
			"여름에 그늘에 있으면 행복한 이유는?(띄어쓰기X)",
			"혀가 거짓말할 때, 하는 말은?(띄어쓰기X)",
			"미국에 비가 내리면?"
		};
		
		//정답 세트
		String[] answer = {
			"Seoul",
			"방학동",
			"해피해서",
			"전혀아닙니다",
			"USB"
		};
		
		@Override
		public void run() {
			System.out.println("Connected: " + socket);
			try {
				
				// 클라이언트와 데이터 송수신을 위한 입출력 스트림 설정
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				
				// 첫 번째 문제를 전송
	            if (currentQuestionIndex < question.length) {
	                out.write("QUESTION:" + question[currentQuestionIndex] + "\n");
	                out.flush();
	            }

	            String message;
	            while ((message = in.readLine()) != null) {
	                if (message.startsWith("ANSWER:")) {
	                    // 클라이언트의 답을 받았을 때
	                    String clientAnswer = message.substring(7);  // "ANSWER:" 부분 제거
	                    if (clientAnswer.equalsIgnoreCase(answer[currentQuestionIndex])) {
	                        score++;  // 정답이면 점수 증가
	                    }
	                    out.write("FEEDBACK:" + (clientAnswer.equalsIgnoreCase(answer[currentQuestionIndex]) ? "Correct!" : "Incorrect.") + "\n");
	                    out.flush();

	                    // 다음 문제로 넘어갈 준비
	                    currentQuestionIndex++;

	                } else if (message.startsWith("NEXT")) {
	                    // "NEXT" 메시지를 받으면 다음 문제를 전송
	                    if (currentQuestionIndex < question.length) {
	                        out.write("QUESTION:" + question[currentQuestionIndex] + "\n");
	                        out.flush();
	                    } else {
	                        out.write("SCORE:" + score + "\n");
	                        out.flush();
	                        break;  // 모든 문제를 다 풀면 종료
	                    }
	                }
	            }
	        }catch(IOException e) {
				System.out.println(e.getMessage());
			}finally {
				try {
					socket.close(); //소켓 닫기
					System.out.println("Closed: "+socket);
				}catch(IOException e) {
					System.out.println("Error clsoing socket");
					}
			}
		}
	}
}
