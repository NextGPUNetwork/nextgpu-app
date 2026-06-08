CREATE SEQUENCE IF NOT EXISTS ai_model_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ai_model
(
    id                    BIGINT DEFAULT nextval('ai_model_seq') NOT NULL,
    uuid                  VARCHAR(38)                            NOT NULL,
    registry              VARCHAR(50),
    model                 VARCHAR(50)                            NOT NULL,
    full_name             VARCHAR(255),
    description           VARCHAR(255),
    tasks                 TEXT,
    required_vram         INTEGER,
    vram_unit             VARCHAR(50),
    size                  INTEGER,
    size_unit             VARCHAR(50),
    type                  VARCHAR(255),
    context_tokens        INTEGER,
    CONSTRAINT pk_ai_model PRIMARY KEY (id)
);
COMMENT ON COLUMN ai_model.id IS 'Primary key';
COMMENT ON COLUMN ai_model.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN ai_model.registry IS 'The registry which lists this model (e.g. Ollama, Huggingface, etc.)';
COMMENT ON COLUMN ai_model.model IS 'Qualified model name, which may be used as a reference in the model registry (e.g. deepseek-r1:1.5b, qwen3-vl:32b, et.c)';
COMMENT ON COLUMN ai_model.full_name IS 'Full name for display on interfaces (e.g. Meta Llama 3.1 - 8B)';
COMMENT ON COLUMN ai_model.description IS 'Description';
COMMENT ON COLUMN ai_model.tasks IS 'The tasks that the model is known to perform (e.g. programming, translation, etc.)';
COMMENT ON COLUMN ai_model.required_vram IS 'Minimum memory of the GPU/CPU required by the model to load';
COMMENT ON COLUMN ai_model.vram_unit IS 'Unit of GPU/CPU memory required';
COMMENT ON COLUMN ai_model.size IS 'The size of the model file';
COMMENT ON COLUMN ai_model.size_unit IS 'Unit of the model size';
COMMENT ON COLUMN ai_model.type IS 'The type of the model (e.g. general, vision, etc.)';
COMMENT ON COLUMN ai_model.context_tokens IS 'The maximum number of tokens in the context window';

ALTER TABLE ai_model
    ADD CONSTRAINT uc_ai_model_model UNIQUE (model);

ALTER TABLE ai_model
    ADD CONSTRAINT uc_ai_model_uuid UNIQUE (uuid);