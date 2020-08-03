#include <Servo.h>  
#include <TimerFreeTone.h>
#include <SoftwareSerial.h>
#include "Sounds.h"

// Constants

#define PIN_LeftWheel   2
#define PIN_RightWheel  3
#define PIN_Red         4
#define PIN_Green       5
#define PIN_Blue        6
#define PIN_Trigger     7
#define PIN_Echo        8
#define PIN_Buzzer      9

const int distanceLimit = 20;           // Front distance limit in cm

SoftwareSerial BTSerial(12, 13); // RX | TX

Servo leftWheelServo;
Servo rightWheelServo;

bool isStopped = true;
bool isRoaming = false;

unsigned int distance;

void setup() 
{
  Serial.begin(9600);

  Serial.println("start");

  BTSerial.begin(9600);

  BTSerial.println("btstart");

  //US
  pinMode(PIN_Trigger, OUTPUT);
  pinMode(PIN_Echo, INPUT);
  
  sing(S_superHappy);
}

void loop() 
{
  if(BTSerial.available() > 0)
  {
    char data =  BTSerial.read();

    Serial.write(data);

    ParseData(data);
  }

  if(Serial.available() > 0)
  {
    char data =  Serial.read();

    Serial.write(data);

    ParseData(data);
  }
  
  if(isRoaming)
  {
    DoRoaming();
  }
}

void ParseData(char data)
{
    switch (data)
    {
        case 'I':
            Serial.println("init");

            sing(S_connection);
           
            break;

       case 'F':
            Serial.println("forward");
            if(isRoaming)
              ToggleRoam();
            
            GoForward();
            break;

       case 'B':
            Serial.println("backward");
            if(isRoaming)
              ToggleRoam();
            
            GoBackward();
            break;

       case 'L':
            Serial.println("left");
            if(isRoaming)
              ToggleRoam();
            
            TurnLeft();
            break;

       case 'R':
            Serial.println("right");
            if(isRoaming)
              ToggleRoam();
            
            TurnRight();
            break;

       case 'S':
            Serial.println("stop");
            Stop();
            break;

       case 'C':
            Serial.println("color");
            analogWrite(PIN_Red, random(0,255));
            analogWrite(PIN_Green, random(0,255));
            analogWrite(PIN_Blue, random(0,255));
            break;

       case 'D':
            Serial.println("no color");
            analogWrite(PIN_Red, 0);
            analogWrite(PIN_Green, 0);
            analogWrite(PIN_Blue, 0);
            break;

       case 'X':
            Serial.println("roaming");
            ToggleRoam();
            break;

       case 'T':
            Serial.println("talk");
            _sing(random(1, 20));
            break;
    }
}

///////////////////////////////////////////////////////////////////
//-- MOTION FUNCTIONS -------------------------------------------//
///////////////////////////////////////////////////////////////////

void GoForward()
{
  if(isStopped)
    Reset();
  
  leftWheelServo.write(0); 
  rightWheelServo.write(180);
}

void GoBackward()
{
  if(isStopped)
    Reset();
  
  leftWheelServo.write(180); 
  rightWheelServo.write(0);
}

void TurnLeft()
{
  if(isStopped)
    Reset();
  
  leftWheelServo.write(0); 
  rightWheelServo.write(0); 
}

void TurnRight()
{
  if(isStopped)
    Reset();
    
  leftWheelServo.write(180); 
  rightWheelServo.write(180); 
}

void Stop()
{
  leftWheelServo.write(90);
  leftWheelServo.detach();
  
  rightWheelServo.write(90);
  rightWheelServo.detach();

  isStopped = true;
}

void Reset()
{
  leftWheelServo.attach(PIN_LeftWheel);
  leftWheelServo.write(90);
  
  rightWheelServo.attach(PIN_RightWheel);
  rightWheelServo.write(90);

  isStopped = false;
}

///////////////////////////////////////////////////////////////////
//-- ROAM FUNCTIONS ---------------------------------------------//
///////////////////////////////////////////////////////////////////

void DoRoaming()
{
  Scan();

  //if the distance is less than distanceLimit, go backward for 1 second, and turn right for 1 second
  if(distance < distanceLimit)
  {
    Stop();
    
    sing(S_surprise);

    delay(1000);
    
    GoBackward();

    delay(1000);

    Stop();

    sing(S_happy_short);

    delay(1000);

    TurnRight();

    delay(1000);
  }
  else
  {
    GoForward();
  }
}

void ToggleRoam()
{                                    
  if(!isRoaming)
  {
    isRoaming = true;
    Serial.println("Activated Roam Mode");
  }
  else
  {
    isRoaming = false;
    Stop();
    Serial.println("De-activated Roam Mode");
  }
}

//This function determines the distance things are away from the ultrasonic sensor
void Scan()                                         
{
  long pulse;
  Serial.println("Scanning distance");
  digitalWrite(PIN_Trigger,LOW);
  delayMicroseconds(5);                                                                              
  digitalWrite(PIN_Trigger,HIGH);
  delayMicroseconds(15);
  digitalWrite(PIN_Trigger,LOW);
  pulse = pulseIn(PIN_Echo,HIGH);
  distance = round( pulse*0.01657 );
  Serial.println(distance);
}

///////////////////////////////////////////////////////////////////
//-- SOUNDS -----------------------------------------------------//
///////////////////////////////////////////////////////////////////

void _tone (float noteFrequency, long noteDuration, int silentDuration){

    // tone(10,261,500);
    // delay(500);

      if(silentDuration==0){silentDuration=1;}

      TimerFreeTone(PIN_Buzzer, noteFrequency, noteDuration);
      delay(noteDuration);       //milliseconds to microseconds
      //noTone(PIN_Buzzer);
      delay(silentDuration);     
}


void bendTones (float initFrequency, float finalFrequency, float prop, long noteDuration, int silentDuration)
{
  //Examples:
  //  bendTones (880, 2093, 1.02, 18, 1);
  //  bendTones (note_A5, note_C7, 1.02, 18, 0);

  if(silentDuration==0){silentDuration=1;}

  if(initFrequency < finalFrequency)
  {
      for (int i=initFrequency; i<finalFrequency; i=i*prop) 
      {
          _tone(i, noteDuration, silentDuration);
      }
  } 
  else
  {
      for (int i=initFrequency; i>finalFrequency; i=i/prop) 
      {
          _tone(i, noteDuration, silentDuration);
      }
  }
}

void _sing(int singId)
{
    switch (singId) 
    {
      case 1: //K 1 
        sing(S_connection);
        break;
      case 2: //K 2 
        sing(S_disconnection);
        break;
      case 3: //K 3 
        sing(S_surprise);
        break;
      case 4: //K 4 
        sing(S_OhOoh);
        break;
      case 5: //K 5  
        sing(S_OhOoh2);
        break;
      case 6: //K 6 
        sing(S_cuddly);
        break;
      case 7: //K 7 
        sing(S_sleeping);
        break;
      case 8: //K 8 
        sing(S_happy);
        break;
      case 9: //K 9  
        sing(S_superHappy);
        break;
      case 10: //K 10
        sing(S_happy_short);
        break;  
      case 11: //K 11
        sing(S_sad);
        break;   
      case 12: //K 12
        sing(S_confused);
        break; 
      case 13: //K 13
        sing(S_fart1);
        break;
      case 14: //K 14
        sing(S_fart2);
        break;
      case 15: //K 15
        sing(S_fart3);
        break;    
      case 16: //K 16
        sing(S_mode1);
        break; 
      case 17: //K 17
        sing(S_mode2);
        break; 
      case 18: //K 18
        sing(S_mode3);
        break;   
      case 19: //K 19
        sing(S_buttonPushed);
        break;                      
      default:
        break;
    }
}

void sing(int songName)
{
  switch(songName)
  {
    case S_connection:
      _tone(note_E5,50,30);
      _tone(note_E6,55,25);
      _tone(note_A6,60,10);
    break;

    case S_disconnection:
      _tone(note_E5,50,30);
      _tone(note_A6,55,25);
      _tone(note_E6,50,10);
    break;

    case S_buttonPushed:
      bendTones (note_E6, note_G6, 1.03, 20, 2);
      delay(30);
      bendTones (note_E6, note_D7, 1.04, 10, 2);
    break;

    case S_mode1:
      bendTones (note_E6, note_A6, 1.02, 30, 10);  //1318.51 to 1760
    break;

    case S_mode2:
      bendTones (note_G6, note_D7, 1.03, 30, 10);  //1567.98 to 2349.32
    break;

    case S_mode3:
      _tone(note_E6,50,100); //D6
      _tone(note_G6,50,80);  //E6
      _tone(note_D7,300,0);  //G6
    break;

    case S_surprise:
      bendTones(800, 2150, 1.02, 10, 1);
      bendTones(2149, 800, 1.03, 7, 1);
    break;

    case S_OhOoh:
      bendTones(880, 2000, 1.04, 8, 3); //A5 = 880
      delay(200);

      for (int i=880; i<2000; i=i*1.04) {
           _tone(note_B5,5,10);
      }
    break;

    case S_OhOoh2:
      bendTones(1880, 3000, 1.03, 8, 3);
      delay(200);

      for (int i=1880; i<3000; i=i*1.03) {
          _tone(note_C6,10,10);
      }
    break;

    case S_cuddly:
      bendTones(700, 900, 1.03, 16, 4);
      bendTones(899, 650, 1.01, 18, 7);
    break;

    case S_sleeping:
      bendTones(100, 500, 1.04, 10, 10);
      delay(500);
      bendTones(400, 100, 1.04, 10, 1);
    break;

    case S_happy:
      bendTones(1500, 2500, 1.05, 20, 8);
      bendTones(2499, 1500, 1.05, 25, 8);
    break;

    case S_superHappy:
      bendTones(2000, 6000, 1.05, 8, 3);
      delay(50);
      bendTones(5999, 2000, 1.05, 13, 2);
    break;

    case S_happy_short:
      bendTones(1500, 2000, 1.05, 15, 8);
      delay(100);
      bendTones(1900, 2500, 1.05, 10, 8);
    break;

    case S_sad:
      bendTones(880, 669, 1.02, 20, 200);
    break;

    case S_confused:
      bendTones(1000, 1700, 1.03, 8, 2); 
      bendTones(1699, 500, 1.04, 8, 3);
      bendTones(1000, 1700, 1.05, 9, 10);
    break;

    case S_fart1:
      bendTones(1600, 3000, 1.02, 2, 15);
    break;

    case S_fart2:
      bendTones(2000, 6000, 1.02, 2, 20);
    break;

    case S_fart3:
      bendTones(1600, 4000, 1.02, 2, 20);
      bendTones(4000, 3000, 1.02, 2, 20);
    break;
  }
} 
