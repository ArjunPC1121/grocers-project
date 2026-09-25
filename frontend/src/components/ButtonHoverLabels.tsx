import { useEffect } from "react";

const labelFor = (element: HTMLButtonElement | HTMLAnchorElement) => {
  const accessibleName = element.getAttribute("aria-label");
  const visibleLabel = element.textContent?.replace(/\s+/g, " ").trim();
  const iconClass = [...element.querySelector("svg")?.classList ?? []]
    .find((name) => name.startsWith("lucide-"))
    ?.replace("lucide-", "")
    .replaceAll("-", " ");

  return accessibleName || visibleLabel || iconClass;
};

/** Adds native hover labels to buttons and links, including controls rendered later in dialogs. */
export default function ButtonHoverLabels() {
  useEffect(() => {
    const addLabels = (root: ParentNode = document) => {
      const elements: Array<HTMLButtonElement | HTMLAnchorElement> = [];
      if (root instanceof HTMLButtonElement || root instanceof HTMLAnchorElement) elements.push(root);
      root.querySelectorAll<HTMLButtonElement | HTMLAnchorElement>("button, a").forEach((element) => elements.push(element));
      elements.forEach((element) => {
        if (!element.title || element.dataset.autoHoverLabel) {
          const label = labelFor(element);
          if (label) {
            element.title = label;
            element.dataset.autoHoverLabel = "true";
          }
        }
      });
    };

    addLabels();
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        const trigger = mutation.target instanceof Element ? mutation.target : mutation.target.parentElement;
        const triggerControl = trigger?.closest<HTMLButtonElement | HTMLAnchorElement>("button, a");
        if (triggerControl) addLabels(triggerControl);
        mutation.addedNodes.forEach((node) => {
          const control = node.parentElement?.closest<HTMLButtonElement | HTMLAnchorElement>("button, a");
          if (control) addLabels(control);
          if (node instanceof HTMLElement) {
            if (node.matches("button, a")) addLabels(node.parentNode ?? document);
            addLabels(node);
          }
        });
      });
    });
    observer.observe(document.body, { childList: true, characterData: true, subtree: true });
    return () => observer.disconnect();
  }, []);

  return null;
}
