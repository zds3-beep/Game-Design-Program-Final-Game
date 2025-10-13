package com.example.finalprojectstorygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.finalprojectstorygame.ui.theme.FinalProjectStoryGameTheme
import android.widget.Button
import android.widget.Toast
import android.widget.TextView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var MagicalPower = 1;
        var RankPower = 1;
        var SocialPower = 1;
        var FinancialPower = 1;
        var EquipmentPower = 1;
        var MartialPower = 1;
        val EquipmentPowerText = findViewById<TextView>(R.id.EquipmentPowerText)
        val FinancialPowerText = findViewById<TextView>(R.id.FinancialPowerText)
        val MagicalPowerText = findViewById<TextView>(R.id.MagicalPowerText)
        val RankPowerText = findViewById<TextView>(R.id.RankPowerText)
        val MartialPowerText = findViewById<TextView>(R.id.MartialPowerText)
        val SocialPowerText = findViewById<TextView>(R.id.SocialPowerText)

        val myButton = findViewById<Button>(R.id.myButton)
        myButton.setOnClickListener {

        }

        val myButton2 = findViewById<Button>(R.id.myButton2)
        myButton2.setOnClickListener {
            EquipmentPower+=5;
            FinancialPower+=5;
            EquipmentPowerText.text = "Equipment Power: $EquipmentPower"
            FinancialPowerText.text = "Financial Power: $FinancialPower"

        }

        val myButton3 = findViewById<Button>(R.id.myButton3)
        myButton3.setOnClickListener {
            SocialPower +=5;
            MartialPower +=5;
            MartialPowerText.text = "Martial Power: $MartialPower"
            SocialPowerText.text = "Social Power: $SocialPower"
        }

        val myButton4 = findViewById<Button>(R.id.myButton4)
        myButton4.setOnClickListener {
            SocialPower +=20;
            RankPower +=20;
            RankPowerText.text = "Rank Power: $RankPower"
            SocialPowerText.text = "Social Power: $SocialPower"
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinalProjectStoryGameTheme {
        Greeting("Android")
    }
}