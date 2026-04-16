# 🚀 StudyAI — Diretrizes do Projeto (MVP)

---

# 🧠 Visão Geral

## 🎯 Objetivo

Desenvolver um SaaS chamado **StudyAI** que utiliza IA para transformar conteúdos acadêmicos em resumos estruturados, claros e otimizados para revisão.

---

## ❗ Problema

Estudantes universitários enfrentam **sobrecarga de conteúdo**, dificultando:

* leitura eficiente
* revisão
* retenção de informação

---

## 💡 Solução

Permitir que o usuário envie:

* PDF (até 10 páginas)
  ou
* texto

E receba um **resumo estruturado padronizado**, gerado por IA.

---

# 👤 Público-Alvo

Estudantes universitários que:

* estudam por PDFs e materiais digitais
* precisam revisar conteúdo rapidamente
* buscam produtividade no estudo

---

# ⚙️ MVP — Escopo Funcional

## 🔁 Fluxo Principal

1. Usuário se cadastra
2. Confirma email com código
3. Realiza login
4. Envia:

   * PDF (até 10 páginas)
   * ou texto
5. Sistema processa conteúdo
6. IA gera resumo estruturado
7. Usuário visualiza o resultado

---

# 🧾 Formato da Resposta (OBRIGATÓRIO)

Toda resposta deve seguir EXATAMENTE o seguinte formato:

1. **Título**
2. **Resumo curto (máx 5 linhas)**
3. **Tópicos principais**
4. **Pontos importantes**
5. **Explicação simplificada**

---

## 📌 Regras da IA

* Linguagem clara e objetiva
* Adequada para nível universitário
* Evitar simplificação excessiva
* Não fugir do formato definido
* Não adicionar seções extras

---

# 🤖 Inteligência Artificial

## 🔧 Tecnologia

* OpenAI API

---

## 📌 Diretrizes

* Uso via prompt estruturado no backend
* Sem treinamento de modelo próprio
* Respostas devem ser armazenadas (evitar reprocessamento)
* Controle de tamanho de entrada obrigatório

---

# 📏 Limitações do MVP

* Apenas PDF com texto extraível
* Máximo: 10 páginas por arquivo
* Sem suporte a PowerPoint (.pptx)
* Sem chat contínuo
* Sem geração de questões ou flashcards

---

# 💰 Monetização

## Plano Free

* 10 usos por mês

---

## Regras

* Reset mensal fixo
* Bloqueio ao atingir limite
* Verificação deve ocorrer antes da chamada da IA

---

# 🧱 Backend

## 🛠 Tecnologias

* Java
* Spring Boot
* Spring Security
* PostgreSQL
* Redis

---

## 📦 Estrutura (alto nível)

* auth → autenticação
* user → usuários
* study → materiais e resumos
* ai → integração com IA
* file → upload
* subscription → planos

---

## 🔐 Segurança

* JWT (autenticação stateless)
* Hash de senha (BCrypt)
* Validação de input
* Rate limiting (anti abuso IA)

---

## 📊 Controle de Uso

Campos necessários:

* monthly_usage_count
* last_reset_date
* plan_type

---

# 🎨 Frontend

## 🛠 Tecnologias

* Next.js
* React
* Tailwind CSS

---

## 📱 Telas

* Login / Cadastro
* Confirmação de email
* Dashboard
* Upload de conteúdo
* Tela de resultado
* Tela de upgrade

---

## 🎯 UX

* Interface simples e limpa
* Feedback visual (loading)
* Resposta renderizada de forma progressiva (opcional)

---

# ☁️ Infraestrutura

## 🛠 Tecnologias

* Docker
* Docker Compose
* AWS (futuro deploy)

---

## 📦 Serviços

* Backend containerizado
* Frontend containerizado
* Banco de dados
* Redis
* Armazenamento de arquivos (S3)

---

# 🧪 Qualidade

* Testes unitários (services)
* Testes de integração (auth + IA)
* Tratamento global de exceções

---

# 📊 Observabilidade (Futuro)

* Logs estruturados
* Monitoramento
* Métricas

---

# 📜 Requisitos Legais

* Política de privacidade
* Termos de uso

---

# 🚫 Fora do Escopo (MVP)

* Chat estilo ChatGPT
* Suporte a PPTX
* IA treinada/customizada
* Sistema avançado de revisão
* Análise baseada em provas reais

---

# 🎯 Objetivo do MVP

Validar:

* se usuários veem valor no resumo estruturado
* se utilizam de forma recorrente
* se estão dispostos a pagar

---

# 🧠 Princípio Fundamental

> Construir o menor produto possível que entregue valor real, com qualidade de produção.

---
