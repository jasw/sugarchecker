# Sugar Checker Idea
To provide another way of monitoring sugar levels for our diabetes patients.
This project builds a Alexa Skills with trigger name 'Sugar Checker'. This skill talks to AWS Lambda function which in turn fetch data off a remote API for the sugar level etc.

## Setup
To run this skill you need to do two things. The first is to deploy the example code in lambda, and the second is to configure the Alexa skill to use Lambda.

### AWS Lambda Setup
1. Go to the AWS Console and click on the Lambda link. Note: ensure you are in us-east or you wont be able to use Alexa with Lambda.
2. Click on the Create Function button.
3. Click Author from scratch.
4. In Configure triggers, add Alexa Skill kit as trigger.
5. Name the Lambda Function "sugar-checker".
6. Select the runtime as Java 8.
7. Build a jar file to upload it into the lambda function. There are two ways:
- Using maven: go to the directory containing pom.xml, and run 'mvn assembly:assembly -DdescriptorId=jar-with-dependencies package'. This will generate a zip file named "sugarchecker-1.0-jar-with-dependencies.jar" in the target directory.
- Using gradle: go to the directory containing build.gradle,  and run 'gradle fatJar'. This will generate a zip file named "sugarchecker-fat-1.0.jar" in the build/libs directory.
8. Upload the jar created in step 7 from the build directory to Lambda.
9. Set the Handler as com.jasons.alexa.sugarchecker.SugarCheckerSpeechletRequestStreamHandler (this refers to the Lambda RequestStreamHandler file in the zip).
10. Choose an existing role - lambda_basic_execution.
11. Increase the Timeout to 30 seconds under Basic Settings.
12. Leave the Advanced settings as the defaults.
13. Click "Next" and review the settings then click "Create Function".
14. Copy the ARN from the top right to be used later in the Alexa Skill Setup.

### Alexa Skill Setup
1. Go to the [Alexa Console](https://developer.amazon.com/edw/home.html) and click Add a New Skill.
2. Set "SugarChecker" as the skill name and "Sugar Checker" as the invocation name, this is what is used to activate your skill. For example you would say: "Alexa, Ask sugar check to check now."
3. Select the Lambda ARN for the skill Endpoint and paste the ARN copied from above. Click Next.
4. Drag the SkillBuilder.json into the Json Editor and click on 'Build Model' 
5. Optional. Go back to the skill Information tab and copy the appId. Paste the appId into the com.jasons.alexa.sugarchecker.SugarCheckerSpeechletRequestStreamHandler.java file for the variable supportedApplicationIds,
   then update the lambda source zip file with this change and upload to lambda again, this step makes sure the lambda function only serves request from authorized source.
6. You are now able to start testing your sample skill! You should be able to go to the [Echo webpage](http://echo.amazon.com/#skills) and see your skill enabled.
7. In order to test it, try to say some of the Sample Utterances from the Examples section below.
8. Your skill is now saved and once you are finished testing you can continue to publish your skill.

## Examples
### One-shot model:
    User:  "Alexa, ask Sugar Checker to check now"
    Alexa: "At 9:15 in the morning the sugar level is 8.5."

### Dialog model:
    User:  "Alexa, open Sugar Checker"
    Alexa: "Welcome to Sugar Checker. Say check now to check sugar level now"
    User:  "Check now"
    Alexa: "At 9:15 in the morning the sugar level is 8.5."
    User:  "Last 4 records
    Alexa: "At 9:15 ...., at 8:40 ...., at .... "
    User: "Thank you jason"
    Alexa: "xxxxx"
