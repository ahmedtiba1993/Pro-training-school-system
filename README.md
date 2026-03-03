# AcademicYearService

Ce module gère les opérations principales liées à la gestion des années scolaires dans le système.

## Fonctions principales (Cas d'utilisation)

### create(AcademicYearRequest request)

**Rôle :**  
Créer une nouvelle année scolaire.

**Règle :**

- Par défaut, l'année scolaire est créée avec `isActive = false`.

---

### getAll()

**Rôle :**  
Récupérer la liste complète de toutes les années scolaires.

**Utilisation :**

- Afficher un tableau des années scolaires
- Remplir un menu déroulant

---

### getById(Long id)

**Rôle :**  
Récupérer les détails d'une année scolaire spécifique.

**Peut inclure :**

- Les semestres
- Les périodes académiques

---

### update(Long id, AcademicYearRequest request)

**Rôle :**  
Modifier les informations d'une année scolaire existante.

**Cas d'utilisation :**

- Modifier le libellé
- Corriger les dates
- Corriger une erreur de saisie

**Condition :**

- L'année ne doit pas encore avoir commencé (selon la logique métier).

---

### activateYear(Long id) *(Fonction critique)*

**Rôle :**  
Passer une année scolaire en `isActive = true`.

**Logique métier :**

- Une seule année scolaire peut être active à la fois.
- L'ancienne année active doit être automatiquement désactivée.

**Processus :**

1. Rechercher l'année scolaire actuellement active.
2. La passer à `isActive = false`.
3. Activer la nouvelle année scolaire (`isActive = true`).
4. Exécuter l'opération dans une transaction.

---

### getActiveYear()

**Rôle :**  
Récupérer l'année scolaire actuellement active.

**Utilisation :**
Cette méthode est utilisée par plusieurs modules :

- Inscription des étudiants
- Gestion des classes
- Paiements
- Planning académique

---

### delete(Long id)

**Rôle :**  
Supprimer une année scolaire.

**Attention :**
La suppression peut être bloquée si :

- Des étudiants sont déjà inscrits dans cette année scolaire.
- Des classes ou des paiements y sont liés.