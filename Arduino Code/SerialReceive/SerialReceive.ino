//#include <SoftwareServo.h>
#include <SoftwareSerial.h>

SoftwareSerial bluetooth(0,1);

int led = 13;

/*

x = analogRead(sensor);
bluetooth.write(x);


*/

void setup()
{

	//Serial.begin(9600);
	bluetooth.begin(9600);
	pinMode(led, OUTPUT);
	bluetooth.println("Ready for command");
}

void loop()
{
	
  if (bluetooth.available() > 0)
	{
		char x = bluetooth.read();

		//bluetooth.println(x);
		if (x == 'd') {
                Serial.println(x);
                
                bluetooth.println("Get d");
             }
             if(x== 'a')
             {
             bluetooth.println("get a");
             }
	
	}

/*
int v =60;
char buf[8];
sprintf(buf, "Ground%d", v);   //String Concatanation with string and int value
bluetooth.println(buf);
delay(2000);
sprintf(buf, "  Left%d", v);
bluetooth.println(buf);
delay(2000);
*/

}
