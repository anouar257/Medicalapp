import { Component, Output, EventEmitter, inject, signal, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PreferencesService } from '../../services/preferences.service';
import { AiOrientationService, AiOrientationResponse, ChatMessage, SpecialtyRecommendation } from '../../services/ai-orientation.service';

@Component({
  selector: 'app-ai-assistant-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant-chat.component.html',
  styleUrls: ['./ai-assistant-chat.component.scss']
})
export class AiAssistantChatComponent {
  readonly prefs = inject(PreferencesService);
  private readonly aiService = inject(AiOrientationService);

  @Output() specialtySelected = new EventEmitter<string>();

  isOpen = signal<boolean>(false);
  isMinimized = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  // User input message
  userMessage = '';

  // Conversation history
  messages = signal<Array<{
    sender: 'user' | 'assistant';
    text: string;
    specialties?: SpecialtyRecommendation[];
    urgency?: string;
    warning?: string;
    needMoreInfo?: boolean;
  }>>([]);

  constructor() {
    effect(() => {
      // Re-evaluate whenever language changes
      const lang = this.prefs.language();
      
      // Use untracked to prevent infinite dependency loop when reading/writing to messages signal
      untracked(() => {
        if (this.messages().length <= 1) {
          this.messages.set([
            {
              sender: 'assistant',
              text: this.getInitialMessage()
            }
          ]);
        }
      });
    });
  }

  getInitialMessage(): string {
    const lang = this.prefs.language();
    if (lang === 'ar') {
      return 'مرحباً 👋 أنا مساعدك للتوجيه الطبي الأولي. صف ببساطة ما تشعر به، وسأساعدك في معرفة التخصص الطبي الأنسب.';
    } else if (lang === 'en') {
      return 'Hello 👋 I’m your medical pre-orientation assistant. Simply describe what you feel, and I’ll help guide you toward the most suitable specialty.';
    } else {
      return 'Bonjour 👋 Je suis votre assistant de pré-orientation médicale. Décrivez simplement ce que vous ressentez, et je vous aiderai à trouver la spécialité la plus adaptée.';
    }
  }

  getSuggestions(): string[] {
    const lang = this.prefs.language();
    if (lang === 'ar') {
      return [
        'أعاني من ألم في الأسنان',
        'أعاني من ألم في البطن',
        'أبحث عن طبيب',
        'لا أعرف أي تخصص أختار'
      ];
    } else if (lang === 'en') {
      return [
        'I have tooth pain',
        'I have stomach pain',
        'I’m looking for a doctor',
        'I don’t know which specialty to choose'
      ];
    } else {
      return [
        "J'ai mal aux dents",
        "J'ai mal au ventre",
        "Je cherche un médecin",
        "Je ne sais pas quelle spécialité choisir"
      ];
    }
  }

  toggleChat(): void {
    this.isOpen.update(val => !val);
    if (this.isOpen()) {
      this.isMinimized.set(false);
      if (this.messages().length === 0) {
        this.messages.set([
          {
            sender: 'assistant',
            text: this.getInitialMessage()
          }
        ]);
      }
    }
  }

  minimizeChat(event: Event): void {
    event.stopPropagation();
    this.isMinimized.update(val => !val);
  }

  closeChat(event: Event): void {
    event.stopPropagation();
    this.isOpen.set(false);
    this.isMinimized.set(false);
  }

  resetChat(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.messages.set([
      {
        sender: 'assistant',
        text: this.getInitialMessage()
      }
    ]);
    this.userMessage = '';
    this.errorMessage.set(null);
  }

  cycleLanguage(event: Event): void {
    event.stopPropagation();
    const current = this.prefs.language();
    let next: 'fr' | 'en' | 'ar' = 'fr';
    if (current === 'fr') next = 'en';
    else if (current === 'en') next = 'ar';
    else if (current === 'ar') next = 'fr';
    this.prefs.setLanguage(next);
  }

  selectSuggestion(suggestion: string): void {
    this.userMessage = suggestion;
    this.sendMessage();
  }

  sendMessage(): void {
    const text = this.userMessage.trim();
    if (!text || text.length < 3 || text.length > 500 || this.isLoading()) {
      return;
    }

    // Get current history before appending user message
    const history: ChatMessage[] = this.messages().map(m => ({
      role: m.sender,
      content: m.text
    }));

    // Add user message to history
    this.messages.update(msgs => [...msgs, { sender: 'user', text }]);
    this.userMessage = '';
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const lang = this.prefs.language();

    this.aiService.getOrientation(text, lang, history).subscribe({
      next: (res: AiOrientationResponse) => {
        this.messages.update(msgs => [...msgs, {
          sender: 'assistant',
          text: res.message,
          specialties: res.specialties,
          urgency: res.urgency,
          warning: res.warning,
          needMoreInfo: res.needMoreInfo
        }]);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.prefs.translate('LANDING.AI_CHAT.ERROR'));
        this.isLoading.set(false);
      }
    });
  }

  onViewDoctors(specialty: string): void {
    this.specialtySelected.emit(specialty);
    // Auto-close chat window on orientation click
    this.isOpen.set(false);
  }
}
