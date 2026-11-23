# FitVerso — InstantHMR + Good-GYM Android MVP

Aplicação Android nativa (Kotlin + Jetpack Compose) unindo recuperação de malha humana 3D em tempo real (**InstantHMR**), contagem automática de repetições e análise biomecânica (**Good-GYM**), biblioteca com **100 exercícios**, gravação de snapshots em **JSON/CSV** e **Coach com voz** (`TextToSpeech` + `SpeechRecognizer`).

## Principais Recursos

- **Pipeline InstantHMR em Tempo Real (`PosePipeline`)**:
  - Câmera ao vivo via CameraX (`ImageAnalysis` RGBA), vídeo quadro a quadro ou fotos.
  - Suporte completo aos grafos ONNX (`nano`, `tiny`, `small`, `medium`, `medium-commercial` com `--fit 0/1/2` + `mhr-body.onnx`) via ONNX Runtime Android, com fallback imediato on-device via ML Kit Pose/Object Detection.
  - Viewport 3D interativo em OpenGL ES 3.0 renderizando a malha MHR de 18.439 vértices e esqueleto MHR70.
- **Contador de Reps & Feedback de Forma (Good-GYM → MHR70)**:
  - Exercícios principais (`Squat`, `Push-up`, `Bicep Curl`, `Lunge`) + catálogo completo com **100 exercícios**.
  - Detecção automática de exercício (`Auto`) ou seleção manual.
  - Avaliação biomecânica em tempo real por ângulos 3D/2D (joelho, cotovelo, quadril, inclinação de tronco, alinhamento de prancha) com indicação visual **Verde / Amarelo / Vermelho**.
- **Snapshots JSON & CSV**:
  - Gravação contínua de timestamps, exercício, fase, ângulos, status de forma e keypoints `joints_2d` / `joints_3d_cam` (MHR70), exportáveis em `.json` ou `.csv`.
- **Coach IA & Voz**:
  - Contagem de repetições e correções faladas em tempo real (`TextToSpeech`) e conversa por voz (`SpeechRecognizer`).
  - Arquitetura preparada via `AiCoachRepository` (atualmente em modo placeholder offline; conectável ao Gemini via Firebase AI Logic quando habilitado).
