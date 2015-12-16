void setup(){
  Serial.begin(9600);  
  pinMode(1,OUTPUT);
}

void loop(){
     Serial.write("1");
     digitalWrite(1,HIGH);
     delay(500);
     Serial.write("0");
     digitalWrite(1,LOW);
     delay(500);
}
