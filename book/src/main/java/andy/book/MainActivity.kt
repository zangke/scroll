package andy.book

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.annotation.RequiresApi
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.toast
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mgr: AudioManager? = null
    private var currentVolume = 0 //媒体播放音量的当前值
    private var telephonyManager: TelephonyManager? = null

    private var position: Int = 0
    private var totals: Int = 0
    private var preferences: SharedPreferences? = null
    private var myTTS: TextToSpeech? = null
    private var circulatePlay = false
    private var textToSpeech = true
    private var speechRate = 80
    private val map = HashMap<String, String>()
    private var subscriptionManager: SubscriptionManager? = null
    private var phoneStateListenerSim1: MyPhoneStateListener? = null
    private var phoneStateListenerSim2: MyPhoneStateListener? = null
    private lateinit var articleGroups: Map<String?, List<Article>>

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) !=
                PackageManager.PERMISSION_GRANTED) {
            toast("缺少修改全局声音的权限！")
            Log.d(TAG, "MODIFY_AUDIO_SETTINGS: " + "no MODIFY_AUDIO_SETTINGS permission")
        } else {
            mgr = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            currentVolume = mgr!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        preferences = getSharedPreferences(DATABASE, AppCompatActivity.MODE_PRIVATE)
        preferences.let {
            position = preferences!!.getInt("position", 0)
            circulatePlay = preferences!!.getBoolean("circulatePlay", false)
            textToSpeech = preferences!!.getBoolean("textToSpeech", true)
            speechRate = preferences!!.getInt("speechRate", 80)

            Log.e(TAG, "onCreate position=" + position)
        }
        totals = ArticleProvider.DATAS.size
        btnFirstWord.setOnClickListener {
            position = 0
            showData()
        }

        btnPreviousWord.setOnClickListener {
            if (position > 0) {
                position--
                showData()
            } else {
                toast("已经到第一个了")
            }
        }

        btnNextWord.setOnClickListener {
            if (position < totals - 1) {
                position++
                showData()
            } else {
                toast("已经到末尾了")
            }
        }

        btnLastWord.setOnClickListener {
            position = totals - 1
            showData()
        }
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TAG)
        //实例并初始化TTS对象
        myTTS = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                //设置朗读语言
                myTTS!!.language = Locale.CHINESE
                //1.0 is normal. lower value decrease the speed and upper value increase
                myTTS!!.setSpeechRate((speechRate / 100).toFloat())
                myTTS!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        Log.d(TAG, "Started TextToSpeech")
                    }

                    override fun onDone(utteranceId: String) {
                        Log.d(TAG, "Done text")
                        //toast("播放完毕！")
                        if (position <= totals - 1) {
                            delay(500)
                            runOnUiThread { autoNext() }
                        }
                    }

                    override fun onError(utteranceId: String) {
                        Log.d(TAG, "Error occurred: " + utteranceId)
                    }
                })
                showData()
            }
        })
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED) {
            toast("缺少读取电话状态权限！")
            Log.d(TAG, "READ_PHONE_STATE: " + "no READ_PHONE_STATE permission")
        } else {
            subscriptionManager = SubscriptionManager.from(this)
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionInfoList = subscriptionManager!!.activeSubscriptionInfoList
            if (subscriptionInfoList != null && subscriptionInfoList.size > 0) {
                (0..subscriptionInfoList.size).forEach {
                    listenPhoneStateChange(it)
                }
            }
        }
        articleGroups = ArticleProvider.articleGroups()
        var list = ArrayList(articleGroups.keys) as MutableList<String>
        Collections.sort(list)
        list.forEach {
            val addSubMenu = nav_view.menu.addSubMenu(it).setIcon(android.R.drawable.ic_menu_edit)
            articleGroups?.get(it)?.forEach { item ->
                addSubMenu.add(item.group, item.id, item.id, item.title).setIcon(R.drawable.article)
            }
        }
        val toggle = ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the app bar.
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_order -> {
                val intent = Intent(this, SettleActivity::class.java)
                intent.putExtra("circulatePlay", circulatePlay)
                intent.putExtra("textToSpeech", textToSpeech)
                intent.putExtra("speechRate", speechRate)
                startActivityForResult(intent, SETTLE_CODE)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showData() {
        scrollDescription!!.scrollTo(0, 0)
        val article = ArticleProvider.DATAS[position]

        var artcleTitle: String? = ""

        with(StringBuffer()) {
            append("" + (position + 1))
            append(". ")
            append(article.title)
        }.let { artcleTitle = it.toString() }

        txtTitle.text = artcleTitle
        txtDescription.text = article.content

        if (textToSpeech && article.content != null) {
            if (myTTS != null) {
                if (myTTS!!.isSpeaking) {
                    myTTS!!.stop()
                    delay(200)
                }
                myTTS!!.speak(artcleTitle, TextToSpeech.QUEUE_FLUSH, null)
                delay(200)
                myTTS!!.speak(article.content, TextToSpeech.QUEUE_ADD, map)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        run breaker@{
            ArticleProvider.DATAS.forEachWithIndex { index, artice ->
                if (artice.id == item.itemId) {
                    position = index
                    return@breaker
                }
            }
        }

        showData()

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun listenPhoneStateChange(simCard: Int) {
        when (simCard) {
            INDEX_SIM1 -> {
                val sub0 = subscriptionManager!!.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM1)
                if (sub0 != null && phoneStateListenerSim1 == null) {
                    phoneStateListenerSim1 = MyPhoneStateListener(sub0.subscriptionId)
                }
                telephonyManager!!.listen(phoneStateListenerSim1, PhoneStateListener.LISTEN_CALL_STATE)
            }
            INDEX_SIM2 -> {
                val sub1 = subscriptionManager!!.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM2)
                if (sub1 != null && phoneStateListenerSim2 == null) {
                    phoneStateListenerSim2 = MyPhoneStateListener(sub1.subscriptionId)
                }
                telephonyManager!!.listen(phoneStateListenerSim2, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    //设置响应intent请求
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode == AppCompatActivity.RESULT_OK) {   //要求的意图成功了
            if (requestCode == SETTLE_CODE) {
                if (intent != null) {
                    circulatePlay = intent.getBooleanExtra("circulatePlay", false)
                    val newTextToSpeech = intent.getBooleanExtra("textToSpeech", true)
                    val newSpeechRate = intent.getIntExtra("speechRate", 80)
                    val isChangeSpeechRate = newSpeechRate != speechRate
                    speechRate = newSpeechRate

                    if (isChangeSpeechRate) {
                        myTTS!!.setSpeechRate((speechRate / 100).toFloat())
                    }
                    if (newTextToSpeech != textToSpeech) {
                        textToSpeech = newTextToSpeech
                        if (textToSpeech) {//现在要播放
                            showData()
                        } else {
                            myTTS!!.stop()
                        }
                    } else {
                        if (textToSpeech && isChangeSpeechRate) {
                            myTTS!!.stop()
                            showData()
                        }
                    }
                }
            }
        }
    }

    internal inner class MyPhoneStateListener(subId: Int) : PhoneStateListener() {
        init {
            ReflectUtil.setFieldValue(this, "mSubId", subId)
        }

        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    currentVolume = mgr!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                    mgr!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND)
                    toast("来电响铃")
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    currentVolume = mgr!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                    mgr!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND)
                    //toast("来电接通")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    mgr!!.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND)
                    //toast("电话空闲")
                }
                else -> {
                    mgr!!.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND)
                }
            }
        }
    }

    /**
     * 延时.
     *
     * @param ms 毫秒数
     */
    private fun delay(ms: Int) {
        try {
            Thread.currentThread()
            Thread.sleep(ms.toLong())
        } catch (e: InterruptedException) {
            Log.e(TAG, e.message)
        }

    }

    /**
     * 自动播放下一篇.
     */
    private fun autoNext() {
        if (position < totals - 1) {
            if (!circulatePlay) {
                position++
            }
//            showData()
        } else {
            position = 0
            //toast("已经到末尾了")
        }
        showData()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        val editor = preferences!!.edit()
        with(editor) {
            putInt("position", position)
            putBoolean("circulatePlay", circulatePlay)
            putBoolean("textToSpeech", textToSpeech)
            putInt("speechRate", speechRate)

            editor.apply()
        }
        Log.e(TAG, "onSaveInstanceState position=" + position)
    }

    override fun onDestroy() {
        if (myTTS != null) {
            myTTS!!.stop()
            myTTS!!.shutdown()
            myTTS = null
        }
        super.onDestroy()
    }

    companion object {
        private val TAG = "MainActivity"
        private val DATABASE = "andy_scroll"
        private val INDEX_SIM1 = 0
        private val INDEX_SIM2 = 1
        private val SETTLE_CODE = 822
    }

}
