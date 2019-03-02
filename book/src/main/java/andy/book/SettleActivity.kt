package andy.book

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settle.*

class SettleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settle)

        val it = intent

        val circulatePlay = it.getBooleanExtra("circulatePlay", false)
        val textToSpeech = it.getBooleanExtra("textToSpeech", true)
        val speechRate = it.getIntExtra("speechRate", 80)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar as ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true)

        swCirculate.isChecked = circulatePlay
        swTextToSpeech.isChecked = textToSpeech
        sbSpeechRate.progress = speechRate

        btnSave.setOnClickListener {
            val it = Intent()
            it.putExtra("circulatePlay", swCirculate!!.isChecked)
            it.putExtra("textToSpeech", swTextToSpeech!!.isChecked)
            it.putExtra("speechRate", sbSpeechRate!!.progress)
            setResult(AppCompatActivity.RESULT_OK, it) // 返回代表成功的结果码, 以及修改的数据
            finish()    //结束活动
        }

        btnCancel.setOnClickListener {
            setResult(AppCompatActivity.RESULT_CANCELED) // 返回代表取消的结果码
            finish()    //结束活动
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        setResult(AppCompatActivity.RESULT_CANCELED)
        finish()
        return true
    }
}
