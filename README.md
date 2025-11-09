# Voice Todo App

An AI-powered voice-controlled todo app for Android. Just speak naturally and let AI handle your tasks.

## Status

**Early MVP** - This is a work in progress. Package naming and branding will be finalized in future releases open for suggestion . Check out our for dev work , any questions or help  [discord](https://discord.gg/6S7HwCxbMy)

## Setup

1. **Download the App**
   - Go to [Releases](../../releases)
   - Download the latest `app-debug.apk`
   - Install on your Android device (requires enabling "Install from Unknown Sources")

2. **Configure API Keys**
   - Open the app and go to Settings
   - **AI Provider** (Required):
     - Enter your OpenAI API key, OR
     - Use any OpenAI-compatible API (like Qwen, Open router , etc.)
     - Set the Base URL if using a compatible provider
   - **Gemini API Key** (Optional but recommended):
     - Used for voice-to-text transcription
     - Get a free key from [Google AI Studio](https://aistudio.google.com/)
     - Without this, voice features won't work

3. **Start Using**
   - Tap the microphone button and start speaking or just chat 
   - Grant microphone permissions when prompted
   - Your todos will be managed through natural conversation

## Features

### Voice Control
- **Natural Speech**: Talk to your app like you'd talk to a person
- **Hands-Free**: Manage todos without typing - perfect for when you're busy
- **Smart Understanding**: AI understands context and intent, not just keywords

### Todo Management
- **Add Tasks**: "Add todo buy groceries"
- **Edit Anything**: Change titles, descriptions, or details on the fly
- **Mark Progress**: Set tasks as in-progress, done, or do-later
- **Set Reminders**: "Remind me tomorrow at 3pm"
- **Delete Tasks**: Remove completed or cancelled todos
- **View All**: See everything organized by status

### Smart Features
- **Multi-Step Commands**: "Add three todos: buy milk, call mom, and finish report" - AI handles all three
- **Permission Control**: Review and approve what the AI does before it happens
- **Always/Never Options**: Trust commonly used actions or block unwanted ones
- **Retry Logic**: Automatically handles temporary connection issues

### Beautiful UI
- **Dark/Light Themes**: Pure black dark mode and clean light mode
- **Chat Bubbles**: See your conversation with AI in a familiar format
- **Visual Status**: Color-coded tasks with icons (✓ done, ▶ in-progress, + pending)
- **Clean Design**: Modern Material Design with smooth animations

### Smart Sessions
- **Multi-Session Chat**: Keep separate conversations for different contexts
- **Message History**: Review past interactions and decisions
- **Tool Call History**: See what actions the AI performed and when

## License

MIT License - Feel free to use, modify, and share!

## Credits

Built with [Koog AI Framework](https://github.com/koog-ai/koog)
