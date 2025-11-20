# Guía de Reglas Firestore (Propuesta)
## Objetivos
- Lectura pública de videos y metadatos (no escritura) para permitir funcionamiento sin autenticación.
- Aislamiento de datos de usuario: cada usuario sólo lee/escribe su propio perfil y progreso.
- Preparar espacio para habilidades/unidades/ejercicios futuros.
- Compatibilidad futura con App Check (se puede exigir request.app != null).

## Reglas DEV abiertas (sin App Check)
```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Videos públicos solo lectura
    match /videos/{videoId} {
      allow read: if true;
      allow write: if false;
    }
    // Perfil usuario y subcolecciones
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
      match /progress/{docId} {
        allow read, write: if request.auth != null && request.auth.uid == uid;
      }
      match /skills/{skillId} {
        allow read, write: if request.auth != null && request.auth.uid == uid;
      }
    }
    // Catálogos públicos
    match /units/{unitId} { allow read: if true; allow write: if false; }
    match /skillsCatalog/{skillId} { allow read: if true; allow write: if false; }
    match /lessons/{lessonId} { allow read: if true; allow write: if false; }
    match /exercises/{exerciseId} { allow read: if true; allow write: if false; }
  }
}
```

## Producción endurecida (cuando App Check activo)
- Reemplazar `allow read: if true` por: `allow read: if request.auth != null && request.app != null`.
- Validar campos en progreso para evitar datos inválidos:
```firestore
allow write: if request.auth.uid == uid &&
  request.resource.data.keys().hasOnly(['lessonId','score','completedAt']) &&
  request.resource.data.score is int && request.resource.data.score >= 0 && request.resource.data.score <= 100 &&
  request.time - request.resource.data.completedAt < duration.value(7, 'd');
```

## PERMISSION_DENIED origen
- Falta de bloques para subcolecciones originalmente. Solucionado agregando match /progress y /skills.

## App Check Debug
1. Activar API en GCP: firebaseappcheck.googleapis.com
2. Capturar token debug en logcat y registrar en consola Firebase (App Check > Debug tokens).
3. Inicialización Kotlin:
```kotlin
FirebaseApp.initializeApp(context)
FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
    DebugAppCheckProviderFactory.getInstance()
)
```

## Próximos pasos
- Subir reglas DEV abiertas ahora mismo.
- Activar App Check en Monitor.
- Migrar a producción endurecida cuando se requiera seguridad adicional.