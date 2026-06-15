import { Component, Output, EventEmitter, inject, signal } from '@angular/core';
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

  toggleChat(): void {
    this.isOpen.update(val => !val);
    if (this.isOpen()) {
      this.isMinimized.set(false);
      if (this.messages().length === 0) {
        this.messages.set([
          {
            sender: 'assistant',
            text: this.prefs.translate('LANDING.AI_CHAT.DESCRIPTION')
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
