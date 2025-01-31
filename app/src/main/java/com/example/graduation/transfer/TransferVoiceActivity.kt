package com.example.graduation.transfer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.graduation.R
import com.example.graduation.databinding.ActivityTransferVoiceBinding
import java.util.Locale

class TransferVoiceActivity : AppCompatActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var mtts: TextToSpeech
    private lateinit var binding: ActivityTransferVoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val writeByHandButton = findViewById<Button>(R.id.write_by_hand_btn)
        writeByHandButton.apply {
            paint.isUnderlineText = true // 밑줄 추가
            setTextColor(ContextCompat.getColor(context, R.color.black)) // 텍스트 색상 설정
        }

        // SharedPreferences에서 소리 on/off 상태 불러오기
        val sharedPreferences = getSharedPreferences("sp1", MODE_PRIVATE)
        val soundState = sharedPreferences.getBoolean("soundState", false)

        mtts = TextToSpeech(this) { //모든 글자를 소리로 읽어주는 tts
            mtts.language = Locale.KOREAN //언어:한국어
        }

        //목소리 입력과 카메라 촬영 중 무엇을 사용하여 계좌번호를 입력하였는지 정보
        val window="voice"
        val editor = sharedPreferences.edit()
        editor.putString("window",window)
        editor.apply()

        //화면 정보 읽기
        mtts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val titleText = binding.titleTv.text.toString()
                val explainText = binding.explainTv.text.toString()
                val buttonExplainText=binding.buttonExplainTv.text.toString()
                val textToSpeak = "$titleText $explainText $buttonExplainText"+"화면 하단의 직접 입력하기 버튼을 눌러 키보드로 입력할 수도 있습니다."
                onSpeech(textToSpeak)
            } else {
                // 초기화가 실패한 경우
                Log.e("TTS", "TextToSpeech 초기화 실패")
            }
        }


        // 권한 설정
        requestPermission()

        // RecognizerIntent 생성
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)    // 여분의 키
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")         // 언어 설정

        //이전 화면 버튼->사진으로 송금하기와 음성으로 송금하기 중 선택 화면으로 이동
        binding.prevBtn.setOnClickListener {
            if (soundState) {
                onSpeech(binding.prevBtn.text)
            }

            val intent = Intent(this, TransferChooseBankActivity::class.java)
            startActivity(intent)
        }

        // <말하기> 버튼 눌러서 음성인식 시작
        binding.speechBtn.setOnClickListener {
            // 새 SpeechRecognizer 를 만드는 팩토리 메서드
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@TransferVoiceActivity)
            speechRecognizer.setRecognitionListener(recognitionListener)    // 리스너 설정
            speechRecognizer.startListening(intent)                         // 듣기 시작
        }

        //손으로 입력하기 버튼 클릭하면 받는 사람 이름 그냥 타자로도 입력 가능
        binding.writeByHandBtn.setOnClickListener {
            if (soundState) {
                onSpeech(binding.writeByHandBtn.text)
            }

            val intent = Intent(this, TransferEnterReceiverByHandActivity::class.java)
            startActivity(intent)
        }

        //다음 버튼 눌러서 송금 확인 화면으로 넘어가기
        binding.nextBtn.setOnClickListener {
            if (soundState) {
                onSpeech(binding.nextBtn.text)
            }

            val intent = Intent(this, TransferEnterAccountNumberActivity::class.java)
            startActivity(intent)
        }

    }

    // 권한 설정 메소드
    private fun requestPermission() {
        // 버전 체크, 권한 허용했는지 체크
        if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(
                this@TransferVoiceActivity,
                Manifest.permission.RECORD_AUDIO
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@TransferVoiceActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO), 0
            )
        }
    }

    // 리스너 설정
    private val recognitionListener: RecognitionListener = object : RecognitionListener {
        // 말하기 시작할 준비가되면 호출
        override fun onReadyForSpeech(params: Bundle) {
            Toast.makeText(applicationContext, "음성인식 시작", Toast.LENGTH_SHORT).show()
            binding.stateTv.text = "이제 말씀하세요!"
        }
        // 말하기 시작했을 때 호출
        override fun onBeginningOfSpeech() {
            binding.stateTv.text = "잘 듣고 있어요."
        }
        // 입력받는 소리의 크기를 알려줌
        override fun onRmsChanged(rmsdB: Float) {}
        // 말을 시작하고 인식이 된 단어를 buffer에 담음
        override fun onBufferReceived(buffer: ByteArray) {}
        // 말하기를 중지하면 호출
        override fun onEndOfSpeech() {
            binding.stateTv.text = "정상적으로 음성 인식이 완료되었습니다."
            //화면 정보 읽기

        }
        // 오류 발생했을 때 호출
        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "퍼미션 없음"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트웍 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER 가 바쁨"
                SpeechRecognizer.ERROR_SERVER -> "서버가 이상함"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말하는 시간초과"
                else -> "알 수 없는 오류임"
            }
            binding.stateTv.text = "에러 발생: $message"
        }
        // 인식 결과가 준비되면 호출
        override fun onResults(results: Bundle) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줌
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            for (i in matches!!.indices) {
                binding.resultTv.text=matches[i]
                val spokenText = matches[i]

                // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줌
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                for (i in matches!!.indices) {
                    binding.resultTv.text = matches[i]
                    val spokenText = matches[i]

                    // 숫자를 제외한 나머지 문자만 추출하여 이름으로 사용
                    val name = spokenText.replace(Regex("[0-9]"), "").trim()

                    // 추출한 이름을 저장
                    val sharedPreferences = getSharedPreferences("transferInfo", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.remove("recipientName") // 키에 해당하는 이전 데이터 삭제
                    editor.putString("recipientName", name)
                    editor.apply()

                    // 음성 인식 성공 시 다음 버튼의 색상을 파랑으로 변경
                    binding.nextBtn.setBackgroundColor(resources.getColor(R.color.blue)) // 파랑색

                }
            }
        }
        // 부분 인식 결과를 사용할 수 있을 때 호출
        override fun onPartialResults(partialResults: Bundle) {}
        // 향후 이벤트를 추가하기 위해 예약
        override fun onEvent(eventType: Int, params: Bundle) {}
    }
    private fun onSpeech(text: CharSequence) {
        mtts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
    }



}