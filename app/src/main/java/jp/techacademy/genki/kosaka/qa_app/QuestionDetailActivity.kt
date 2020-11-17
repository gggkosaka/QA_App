package jp.techacademy.genki.kosaka.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*
import com.google.firebase.database.ValueEventListener

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = "[質問詳細_QuestionDetailActivity]" + mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // TODO:
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        // お気に入りボタン
        favorite.setOnClickListener{

            // Firebaseに保存する qaapp/favorites/ユーザid/質問id
            val favoriteRef = dataBaseReference.child(FavoritesPATH).child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)

            //---------------------------------------
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    if(data == null) {
                        // dataがnull（＝お気に入りに１件も無し）
                        favorite.setImageResource(R.drawable.ic_favorite)

                        val data = HashMap<String, String>()
                        //data["title"] = mQuestion.title
                        data["genre"] = mQuestion.genre.toString()
                        favoriteRef.setValue(data)

                    } else {
                        // データが存在しているので、画像差し替えて削除
                        favorite.setImageResource(R.drawable.ic_favorite_border)
                        favoriteRef.removeValue()
                    }
                    //Log.d("_dev", "質問ID：" + data!!["questionUid"] as String)
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
            //---------------------------------------

        }

    }

    // onStartから変更
    override fun onResume() {
        super.onResume()

        if(FirebaseAuth.getInstance().currentUser == null){
            // ログインしていなければお気に入りボタン非表示
            favorite.hide()

        } else {
            // FirebaseAuthのオブジェクトを取得する qaapp/favorites/ユーザid/質問id
            val userRef = FirebaseDatabase.getInstance().reference.child(FavoritesPATH).child(FirebaseAuth.getInstance().currentUser!!.uid).child(mQuestion.questionUid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?

                    // ボタン画像の判定
                    if(data == null) {
                        // dataがnull（＝お気に入りに１件も無し）
                        favorite.setImageResource(R.drawable.ic_favorite_border)
                    } else {

                        favorite.setImageResource(R.drawable.ic_favorite)
                    }
                    //Log.d("_dev", "質問ID：" + data!!["questionUid"] as String)
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })

            // お気に入りボタン表示
            favorite.show()

        }
    }

}
