package com.jack5.palindrompit

import _root_.android.app.Activity
import _root_.android.os.Bundle
import android.widget._
import android.text.{Editable, TextWatcher}
import android.util.{TypedValue, Log}
import scala.collection.mutable._
import scala.util.control.Breaks._

import scala.actors.Actor
import android.view.{Gravity, ViewGroup, View, WindowManager}
import android.graphics.Color
import android.view.View.OnClickListener

object Logic {
  val allPalCache = new HashMap[Long, ArrayBuffer[Long]]
    with SynchronizedMap[Long, ArrayBuffer[Long]]
  val pairsCache = new HashMap[Long, ArrayBuffer[Long]]
    with SynchronizedMap[Long, ArrayBuffer[Long]]

  def priceToCents(price: String): Long = {
    val parts = price.split("\\.")
    var ret = 0L
    if (parts.length >= 1 && parts(0) != "") {
      val dollarsPlace = parts(0).toLong
      if (dollarsPlace >= 1000000) {
        throw new IllegalArgumentException("No prices higher than $1 million")
      }
      ret += 100 * dollarsPlace
    }
    if (parts.length >= 2 && parts(1) != "") {
      val centsPlace = parts(1).toLong
      if (centsPlace >= 100) {
        throw new IllegalArgumentException("Cents place too long")
      }
      // Treat 1.5 as 1.50
      ret += centsPlace
    }
    ret
  }

  def formatPrice(cents: Long): String = {
    if (cents >= 100) {
      return "$%d.%02d".format(cents/100, cents%100)
    } else {
      return "%02d¢".format(cents)
    }
  }

  def isPalindrome(cents: Long): Boolean = {
    // Deal with the leading zero on a single-digit cent value
    val s = ("%02d" format cents)
    s == s.reverse
  }

  def allPalindromes(length: Int): ArrayBuffer[Long] = {
    if (allPalCache.contains(length)) {
      return allPalCache(length)
    }

    val ret = new ArrayBuffer[Long]

    if (length < 2) {
      // There are no palindromes of length 1, because we're counting cents
      // and there's a leading zero.
      return ret
    }

    val partLen = (length + 1) / 2
    val evenLen = (length % 2 == 0)
    for (part <- 1L until math.pow(10, partLen).toLong) {
      if (part % 10 != 0) {
        val s = ("%0" + partLen + "d").format(part)
        var palindrome = 0L
        if (evenLen) {
          palindrome = (s.reverse + s).toLong
        }
        else {
          palindrome = (s.reverse.slice(0, partLen-1) + s).toLong
        }
        ret.append(palindrome)
      }
    }
    allPalCache(length) = ret.sorted
    return allPalCache(length)
  }

  def palindromePairs(cents: Long): ArrayBuffer[Long] = {
    if (pairsCache.contains(cents)) {
      return pairsCache(cents)
    }
    val pairs = ArrayBuffer[Long]()
    breakable {
      for (length <- 1 to cents.toString.length) {
        for (pal <- allPalindromes(length)) {
          if (pal >= cents) {
            // No tips higher than the input amount.
            break
          }
          if (isPalindrome(cents + pal)) {
            pairs.append(pal)
          }
        }
      }
    }
    pairsCache(cents) = pairs
    return pairs
  }

  class Tip {
    var percentage = ""
    var tip = ""
    var total = ""
  }

  class TipResult {
    var error: String = ""
    var tips = new ArrayBuffer[Tip]
  }

  def tipResultFromPrice(price: String): TipResult = {
    var result = new TipResult
    var cents = 0L
    try {
      cents = priceToCents(price)
    } catch {
      case _ => {
        result.error = "Check yourself before you wreck yourself!"
        return result
      }
    }

    val pairs = palindromePairs(cents)
    if (pairs.length == 0) {
      result.error = "No solution."
    } else {
      for (pair <- pairs) {
        val tip = new Tip
        tip.tip = formatPrice(pair)
        tip.total = formatPrice(cents + pair)
        tip.percentage = "%" + (pair * 100 / cents)
        result.tips.append(tip)
      }
    }
    return result
  }
}

class PalindromeWorker(mainActivity: MainActivity, id: Int) extends Actor {
  def act() {
    val price = mainActivity.input.getText().toString()
    val result = Logic.tipResultFromPrice(price)
    mainActivity.runOnUiThread(new Runnable {
      def run() {
        mainActivity.submitWork(id, result)
      }
    })
  }
}

class MainActivity extends Activity with TypedActivity {

  var input: EditText= null
  var output_text: TextView = null
  var output_table: TableLayout = null
  var clear_button: Button = null
  var currentJob = 0

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    // Keyboard isn't showing by default. This seems to be a workaround.
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

    input = findView(TR.input)
    output_text = findView(TR.output_text)
    output_table = findView(TR.output_table)
    clear_button = findView(TR.clear_button)

    val thisActivity = this
    input.addTextChangedListener(new TextWatcher {
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        currentJob += 1
        val text = input.getText.toString
        if (text == "" || text == ".") {
          setOutputText("")
          return
        }

        setOutputText("Thinking...")
        val worker = new PalindromeWorker(thisActivity, currentJob)
        worker.start
      }
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    })

    clear_button.setOnClickListener(new OnClickListener {
      def onClick(p1: View) {
        input.setText("")
      }
    })
  }

  def setOutputText(text: String) {
    output_text.setText(text)
    output_text.setVisibility(View.VISIBLE)
    output_table.setVisibility(View.GONE)
  }

  def makeRow(s1: String, s2: String, s3: String): TableRow = {
    def makeTextView(text: String): TextView = {
      val view = new TextView(this)
      view.setText(text)
      view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18)
      view.setLayoutParams(new TableRow.LayoutParams(
          0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
      view.setGravity(Gravity.CENTER)
      view
    }
    val row = new TableRow(this)
    row.addView(makeTextView(s1))
    row.addView(makeTextView(s2))
    row.addView(makeTextView(s3))
    row
  }

  def submitWork(job: Int, work: Logic.TipResult) {
    if (job != currentJob) {
      return
    }

    if (work.error != "") {
      setOutputText(work.error)
    } else {
      output_text.setVisibility(View.GONE)
      output_table.setVisibility(View.VISIBLE)
      output_table.removeAllViews()
      val headerRow = makeRow("Percentage", "Tip", "Total")
      output_table.addView(headerRow)
      var highlightRow = true
      for (tip <- work.tips) {
        val row = makeRow(tip.percentage, tip.tip, tip.total)
        if (highlightRow) {
          row.setBackgroundColor(Color.parseColor("#222222"))
        }
        highlightRow = !highlightRow
        output_table.addView(row)
      }
    }
  }
}
