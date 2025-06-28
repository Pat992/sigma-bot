# Sigma Bot

*Supposed to be a quick 1-hour coding adventure, it was not.*

## Content

1. Prerequisites
2. Settings
3. Run

## 1. Prerequisites

- OpenAI Subscription
- Create a .env file and add the OpenAI token, check `template.env` for an example.
- If you want to use the AI in a meeting or call you need to use Linux as well as PulseAudio,
  because the app will create virtual microphones and speaker to pipe audio from and to the AI.

## 2. Settings

When you run the app there are some settings that can be configured.

### 3. Audio method

**Direct Chat:** Chat directly with the AI, should work out-of-the-box and all OS.

**Audio Pipe:** Use the AI in a meeting or call, some prerequisites are needed

- Only works on Linux with PulseAudio
- Change audio settings in the meeting:
    - *AI_In_Speaker_For_Meeting* for Output/Speaker
    - *AI_Out_Microphone_For_Meeting* for Input/Microphone

### Model selection

Allows you to choose the model, will use **gpt-4o-realtime-preview** if you press enter or add an invalid input.

### Voice selection

Allows you to choose the voice, will use **Verse** if you press enter or add an invalid input.

### Language selection

Allows you to choose the language, will use **english** if you press enter without any text.

### Character selection

Allows you to choose the character/instructions, will use **Default Bot** if you press enter
or add an invalid input. You can also add more characters in `src.main.kotlin.data.CharacterEnum`.

## Run

I advise to use IntelliJ to run and build the app.

You can use `build.sh` to build an app-image with **jpackage**, while
you can run the app in Windows, the script will not work, you might have to write one yourself.