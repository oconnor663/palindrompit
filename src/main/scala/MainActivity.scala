package com.jack5.palindrompit

import _root_.android.app.Activity
import _root_.android.os.Bundle
import android.widget.{TextView, EditText}
import android.text.{Editable, TextWatcher}
import android.util.Log
import scala.collection.mutable._
import scala.util.control.Breaks._

import scala.actors.Actor
import android.view.WindowManager

object Logic {
  val allPalCache = new HashMap[Int, ArrayBuffer[Int]]
    with SynchronizedMap[Int, ArrayBuffer[Int]]
  val pairsCache = new HashMap[Int, ArrayBuffer[Int]]
    with SynchronizedMap[Int, ArrayBuffer[Int]]

  def priceToCents(price: String): Int = {
    val parts = price.split("\\.")
    var ret = 0
    if (parts.length >= 1 && parts(0) != "") {
      val dollarsPlace = parts(0).toInt
      if (dollarsPlace >= 1000000) {
        throw new IllegalArgumentException("No prices higher than $1 million")
      }
      ret += 100 * dollarsPlace
    }
    if (parts.length >= 2 && parts(1) != "") {
      val centsPlace = parts(1).toInt
      if (centsPlace >= 100) {
        throw new IllegalArgumentException("Cents place too long")
      }
      // Treat 1.5 as 1.50
      ret += centsPlace
    }
    ret
  }

  def formatPrice(cents: Int): String = {
    if (cents >= 100) {
      return "$%d.%02d".format(cents/100, cents%100)
    } else {
      return ".%02d".format(cents)
    }
  }

  def isPalindrome(cents: Int): Boolean = {
    // Deal with the leading zero on a single-digit cent value
    val s = ("%02d" format cents)
    s == s.reverse
  }

  def allPalindromes(length: Int): ArrayBuffer[Int] = {
    if (allPalCache.contains(length)) {
      return allPalCache(length)
    }

    val ret = new ArrayBuffer[Int]

    if (length < 2) {
      // There are no palindromes of length 1, because we're counting cents
      // and there's a leading zero.
      return ret
    }

    val partLen = (length + 1) / 2
    val evenLen = (length % 2 == 0)
    for (part <- 1 until math.pow(10, partLen).toInt) {
      if (part % 10 != 0) {
        val s = ("%0" + partLen + "d").format(part)
        var palindrome = 0
        if (evenLen) {
          palindrome = (s.reverse + s).toInt
        }
        else {
          palindrome = (s.reverse.slice(0, partLen-1) + s).toInt
        }
        ret.append(palindrome)
      }
    }
    allPalCache(length) = ret.sorted
    return allPalCache(length)
  }

  def palindromePairs(cents: Int): ArrayBuffer[Int] = {
    if (pairsCache.contains(cents)) {
      return pairsCache(cents)
    }
    val pairs = ArrayBuffer[Int]()
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

  def outputFromInput(input: String): String = {
    var cents = 0
    try {
      cents = priceToCents(input)
    } catch {
      case _ => return "Check yourself before you wreck yourself!"
    }

    val pairs = palindromePairs(cents)
    var text = ""
    if (pairs.length == 0) {
      text = "no solution"
    } else {
      for (pair <- pairs) {
        val total = cents + pair
        val percentage = pair * 100 / cents
        text += percentage + "% " + formatPrice(pair) + " " + formatPrice(total) + "\n"
      }
    }
    text
  }
}

class PalindromeWorker(mainActivity: MainActivity, id: Int) extends Actor {
  def act() {
    val price = mainActivity.input.getText().toString()
    val output = Logic.outputFromInput(price)
    mainActivity.runOnUiThread(new Runnable {
      def run() {
        mainActivity.submitWork(id, output)
      }
    })
  }
}

class MainActivity extends Activity with TypedActivity {

  var input: EditText= null
  var output: TextView = null
  var currentJob: Int = 0

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    // Keyboard isn't showing by default. This seems to be a workaround.
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

    input = findView(TR.input)
    output = findView(TR.output)

    val thisActivity = this
    input.addTextChangedListener(new TextWatcher {
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        currentJob += 1
        val text = input.getText.toString
        if (text == "" || text == ".") {
          output.setText("")
          return
        }

        output.setText("Thinking...")
        val worker = new PalindromeWorker(thisActivity, currentJob)
        worker.start
      }
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    })
  }

  def submitWork(job: Int, work: String) {
    if (job == currentJob) {
        output.setText(work)
    }
  }
}
