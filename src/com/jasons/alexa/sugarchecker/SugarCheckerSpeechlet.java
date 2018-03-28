/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.jasons.alexa.sugarchecker;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.jasons.alexa.sugarchecker.WebSiteFetcher.Result;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SugarCheckerSpeechlet implements SpeechletV2 {
    private static final Logger log = LoggerFactory.getLogger(SugarCheckerSpeechlet.class);

    private static final String SLOT_NUMBER = "Number";


    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch timezone={}, requestId={}, sessionId={}", TimeZone.getDefault(), requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());

        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();


        Intent intent = request.getIntent();
        String intentName = intent.getName();
        Map<String, Slot> slots = intent.getSlots();
        Map<String, String> slotsDescrition = new HashMap<>();
        for (Map.Entry<String, Slot> entry : slots.entrySet()) {
            slotsDescrition.put(entry.getKey(), entry.getValue().getName()+":"+entry.getValue().getValue());
        }
        log.debug("onIntent:"+intent.getSlots()+" name = "+intentName+" intent value"+ slotsDescrition+" session:"+session.getAttributes());

        if ("OneshotIntent".equals(intentName)) {
            return handleOneshotRequest(intent, session);
        } else if ("EasterEggIntent".equals(intentName)) {
            return handleEasterEgg(intent, session);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return handleHelpRequest();
        } else if ("AMAZON.StopIntent".equals(intentName) || "ByeIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            String errorSpeech = "This is unsupported.  Please try something else.";
            return newAskResponse(errorSpeech, errorSpeech);
        }
    }

    private SpeechletResponse handleEasterEgg(Intent intent, Session session) {

        return newAskResponse("You are welcome! What can I say? I am just a cool guy who always helps. ", "You know, my bitcoin wallet address is ooxxf, just kidding. Keep up the good work, oliver! ");
    }

    private SpeechletResponse handleOneshotRequest(Intent intent, Session session) {
        WebSiteFetcher fetcher = new WebSiteFetcher();
        try {
            List<Result> results = fetcher.processSite();
            Slot slot = intent.getSlot(SLOT_NUMBER);
            String text = "";
            if(has(slot)){ // wanted last few records
                String number = slot.getValue();
                int num = Integer.parseInt(number);
                List<Result> lastFew = results.subList(0, num );
                for (Result result : lastFew) {
                    text += toText(result)+" \n";
                }
            }else{ // wanted only the last record
                Result next = results.iterator().next();
                text = toText(next);

            }
            return newAskResponse(text, "");
        } catch (IOException e) {
            log.error("error fetching website, ",e);
            return newAskResponse("Error retrieving data from remote website, please try again later. ","");

        }
    }

    private String toText(Result next) {
        return "At "+ next.getTime()+". The sugar level is "+ next.getSugarLevel()+". ";
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    private SpeechletResponse getWelcomeResponse() {
        String checkNow = "Say check now to Check the latest Sugar Level Now";
        String speechOutput = "<speak>"
                + "Welcome to Sugar Checker. "
                //+ "<audio src='https://s3.amazonaws.com/ask-storage/tidePooler/OceanWaves.mp3'/>"
                + checkNow
                + "</speak>";
        String repromptText =" You can also check the last few records. For example, say last 3 records. "+
                 checkNow;

        return newAskResponse(speechOutput, true, repromptText, false);
    }

    private SpeechletResponse handleHelpRequest() {
        String checkNow = "Say check now to Check the latest Sugar Level Now";
        String repromptText =" You can also check the last few records. For example, say last 3 records. or "+checkNow;


        return newAskResponse(checkNow, repromptText);
    }




    /**
     * Wrapper for creating the Ask response from the input strings with
     * plain text output and reprompt speeches.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param isOutputSsml
     *            whether the output text is of type SSML
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml
     *            whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
            String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }

    private boolean has(Slot slot) {

        return slot!=null && slot.getValue()!=null;
    }


}
