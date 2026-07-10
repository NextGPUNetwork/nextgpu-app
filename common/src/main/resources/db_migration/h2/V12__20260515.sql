ALTER TABLE ai_model
    ADD files JSON;
COMMENT
ON COLUMN ai_model.files IS 'The files required by the model for ComfyUI workflow';

ALTER TABLE ai_model
    ADD sampler VARCHAR(255);
COMMENT
ON COLUMN ai_model.sampler IS 'The sampler for ComfyUI workflow';

ALTER TABLE ai_model
    ADD scheduler VARCHAR(255);
COMMENT
ON COLUMN ai_model.scheduler IS 'The scheduler for ComfyUI workflow';

ALTER TABLE ai_model
    ALTER COLUMN description TYPE TEXT
