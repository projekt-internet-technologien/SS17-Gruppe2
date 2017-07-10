float val = 0.0;
int analogPin = 3;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);

}

void loop() {
  // put your main code here, to run repeatedly:
  val = analogRead(analogPin);
  float u = val * 5.0 / 1023.0;
  float r = 4700.0 * u /(5.0 - u);
  float e = pow(r/1000.0, -1.31022)*210.9143;
  Serial.println(e);
}
