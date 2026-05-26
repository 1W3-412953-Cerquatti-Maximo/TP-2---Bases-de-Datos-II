import { Component, ElementRef, HostListener, computed, inject, input, output, signal } from '@angular/core';

export interface SelectOption {
  value: string;
  label: string;
}

/**
 * Dropdown oscuro reutilizable (sin librerías). Reemplaza al <select> nativo
 * para que el panel de opciones respete el tema NexoVeraz en todos los SO
 * (el popup nativo de Windows no se puede estilar y aparece blanco).
 */
@Component({
  selector: 'nv-select',
  imports: [],
  templateUrl: './select-menu.html',
  styleUrl: './select-menu.scss'
})
export class SelectMenu {
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  options = input<SelectOption[]>([]);
  value = input<string>('');
  placeholder = input<string>('Seleccionar');
  ariaLabel = input<string>('');
  valueChange = output<string>();

  open = signal(false);

  selectedLabel = computed(() => {
    const found = this.options().find(o => o.value === this.value());
    return found ? found.label : this.placeholder();
  });

  toggle(): void {
    this.open.update(v => !v);
  }

  choose(value: string): void {
    if (value !== this.value()) {
      this.valueChange.emit(value);
    }
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open.set(false);
  }
}
